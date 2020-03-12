package io.protop.core.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.protop.core.Context;
import io.protop.core.Environment;
import io.protop.core.auth.AuthService;
import io.protop.core.cache.CacheService;
import io.protop.core.cache.CachedProjectsMap;
import io.protop.core.error.PackageNotFound;
import io.protop.core.error.ServiceException;
import io.protop.core.logs.Logger;
import io.protop.core.manifest.AggregatedManifest;
import io.protop.core.manifest.DependencyMap;
import io.protop.core.manifest.Manifest;
import io.protop.core.manifest.ProjectCoordinate;
import io.protop.core.version.Version;
import io.protop.utils.HttpUtils;
import io.protop.utils.RegistryUtils;
import io.reactivex.Maybe;
import io.reactivex.Single;
import lombok.AllArgsConstructor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Resolves dependencies if they have been published to a registry.
 */
@AllArgsConstructor
public class ExternalDependencyResolver implements DependencyResolver {

    private static final Logger logger = Logger.getLogger(ExternalDependencyResolver.class);

    private final AuthService<?> authService;
    private final CacheService cacheService;
    private final Context context;

    @Override
    public String getShortDescription() {
        return "registered dependencies";
    }

    @Override
    public Single<Map<ProjectCoordinate, Version>> resolve(
            Path dependencyDir, Map<ProjectCoordinate, Version> projectDependencies) {
        return Single.create(emitter -> {
            Map<ProjectCoordinate, Map<Version, Path>> cachedProjects = loadCachedProjects();
            Map<ProjectCoordinate, Version> aggregatedDependencies = aggregateDependencies(
                    projectDependencies, extractManifestsFromCache(cachedProjects));

            logger.info("Aggregated dependencies: " + new ObjectMapper().writeValueAsString(aggregatedDependencies));

            Set<ProjectCoordinate> resolved = new HashSet<>();
            aggregatedDependencies.forEach((coordinate, version) -> {
                Map<Version, Path> cachedVersions = cachedProjects.computeIfAbsent(
                        coordinate, coord -> new HashMap<>());

                AtomicReference<Path> path = new AtomicReference<>();
                if (cachedVersions.containsKey(version)) {
                    path.set(cachedVersions.get(version));
                } else {
                    logger.info("Not found; attempting to retrieve from registry.");
                    try {
                        Path cached = retrieveAndCache(coordinate, version).blockingGet();
                        path.set(cached);
                    } catch (Throwable t) {
                        // TODO something else?
                    }
                }

                Path sourceDir = path.get();
                if (Objects.nonNull(sourceDir)) {
                    try {
                        SyncUtils.createSymbolicLink(dependencyDir, coordinate, sourceDir);
                        resolved.add(coordinate);
                    } catch (Exception e) {
                        // TODO handle better
                        throw new ServiceException("Unexpectedly failed to resolve external dependencies.", e);
                    }
                }
            });

            resolved.forEach(aggregatedDependencies::remove);
            emitter.onSuccess(aggregatedDependencies);
        });
    }

    private Map<ProjectCoordinate, Map<Version, Path>> loadCachedProjects() {
        return CachedProjectsMap.load().blockingGet()
                .getProjects();
    }

    private Map<ProjectCoordinate, Map<Version, Manifest>> extractManifestsFromCache(
            Map<ProjectCoordinate, Map<Version, Path>> cachedProjects) {
        Map<ProjectCoordinate, Map<Version, Manifest>> output = new HashMap<>();
        cachedProjects.forEach((coordinate, versionPathMap) -> {
            Map<Version, Manifest> versionManifestMap = new HashMap<>();
            versionPathMap.forEach((version, path) -> {
                // TODO handle better
                Manifest manifest = Manifest.from(path).orElseThrow();
                versionManifestMap.put(version, manifest);
            });
            output.put(coordinate, versionManifestMap);
        });
        return output;
    }

    private Map<ProjectCoordinate, Version> aggregateDependencies(
            Map<ProjectCoordinate, Version> projectDependencies,
            Map<ProjectCoordinate, Map<Version, Manifest>> manifestsForReference) {
        Map<ProjectCoordinate, Version> aggregated = new HashMap<>();

        Queue<Map.Entry<ProjectCoordinate, Version>> unchecked =
                new LinkedList<>(projectDependencies.entrySet());

        while (!unchecked.isEmpty()) {
            Map.Entry<ProjectCoordinate, Version> entry = unchecked.poll();
            ProjectCoordinate coordinate = entry.getKey();
            Version version = entry.getValue();

            logger.info("checking dependency: {} {}.", coordinate, version);

            Map<Version, Manifest> knownVersions = manifestsForReference.computeIfAbsent(
                    coordinate, c -> new HashMap<>());

            AtomicReference<Manifest> manifest = new AtomicReference<>();
            if (knownVersions.containsKey(version)) {
                manifest.set(knownVersions.get(version));
            } else {
                // try to retrieve the manifest from the network
                AggregatedManifest aggregatedManifest = retrieveAggregatedManifest(coordinate)
                        .blockingGet();
                Map<Version, Manifest> aggregatedVersions = aggregatedManifest.getVersions();
                manifestsForReference.put(coordinate, aggregatedVersions);
                if (aggregatedVersions.containsKey(version)) {
                    manifest.set(aggregatedVersions.get(version));
                }
            }

            Manifest resolvedManifest = manifest.get();
            if (Objects.isNull(resolvedManifest)) {
                throw new PackageNotFound(coordinate, version);
            } else {
                try {
                    logger.info("Dependencies: " + Environment.getInstance().getObjectMapper()
                            .writeValueAsString(resolvedManifest.getDependencies()));
                } catch (Exception e) {
                    // ...
                }

                if (!aggregated.containsKey(coordinate)
                        || (aggregated.get(coordinate).compareTo(version) < 0)) {
                    aggregated.put(coordinate, version);
                    DependencyMap dependencyMap = resolvedManifest.getDependencies();
                    if (Objects.nonNull(dependencyMap)) {
                        dependencyMap.getValues().forEach((coordinate1, version1) -> {
                            logger.info("now checking sub-dependency: {} {}.", coordinate1, version1);
                            if (!aggregated.containsKey(coordinate1)
                                    || (aggregated.get(coordinate1).compareTo(version1) < 0)) {
                                logger.info("adding to unchecked...");
                                unchecked.add(new HashMap.SimpleImmutableEntry<>(coordinate1, version1));
                            }
                        });
                    }
                }
            }
        }

        return aggregated;
    }

    private Single<AggregatedManifest> retrieveAggregatedManifest(ProjectCoordinate coordinate) {
        return Single.create(emitter -> {
            URI uri = RegistryUtils.createManifestUri(getRegistryUri(), coordinate);
            HttpResponse response = createHttpClient()
                    .execute(new HttpGet(uri));

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                String message = String.format("Did not find project in registry: %s.", coordinate);
                logger.error(message);
                emitter.onError(new ServiceException(message));
            } else {
                try {
                    ObjectMapper objectMapper = Environment.getInstance().getObjectMapper();
                    String stringEntity = EntityUtils.toString(response.getEntity());
                    AggregatedManifest aggregatedManifest = objectMapper.readValue(
                            stringEntity, AggregatedManifest.class);
                    emitter.onSuccess(aggregatedManifest);
                } catch (Exception e) {
                    logger.error("Failed to parse manifest for " + coordinate, e);
                    emitter.onError(new ServiceException("Failed to parse manifest for " + coordinate));
                }
            }
        });
    }

    // TODO i don't like the side-effect-ness of this. However, it is important that the map is updated since it is
    //  a reference of the cache used for all resolved dependencies
    private Maybe<Path> retrieveAndCache(ProjectCoordinate coordinate, Version version) {
        return Maybe.create(emitter -> {
            // TODO handle uri composition more cleanly
            URI uri = RegistryUtils.createTarballUri(
                    getRegistryUri(),
                    coordinate,
                    version);
            HttpResponse response = createHttpClient()
                    .execute(new HttpGet(uri));

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                logger.error("Failed to retrieve package. URI: {}. Status code: {}.",
                        uri,
                        statusCode);
                // TODO handle better, i.e. map to a specific exception based on the response/code from the registry.
                emitter.onComplete();
            } else {
                Path path = cacheService.cache(coordinate, version, response.getEntity().getContent())
                        .blockingGet();

                logger.info("Happy path achieved.");
                emitter.onSuccess(path);
            }
        });
    }

    private URI getRegistryUri() {
        return context.getRc().getRepositoryUri();
    }

    private HttpClient createHttpClient() {
        return Optional.ofNullable(authService.getStoredToken(getRegistryUri()).blockingGet())
                .map(HttpUtils::createHttpClientWithToken)
                .orElseGet(HttpUtils::createHttpClient);
    }
}
