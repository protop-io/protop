package io.protop.core.sync;

import io.protop.core.Context;
import io.protop.core.auth.AuthService;
import io.protop.core.auth.AuthToken;
import io.protop.core.cache.CacheService;
import io.protop.core.cache.CachedProjectsMap;
import io.protop.core.error.ServiceException;
import io.protop.core.logs.Logger;
import io.protop.core.manifest.ProjectCoordinate;
import io.protop.utils.HttpUtils;
import io.protop.utils.RegistryUtils;
import io.protop.utils.UriUtils;
import io.protop.version.Version;
import io.reactivex.Maybe;
import io.reactivex.Single;

import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import lombok.AllArgsConstructor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

/**
 * Resolves dependencies published to a registry.
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
            Path dependencyDir, Map<ProjectCoordinate, Version> unresolvedDependencies) {
        return Single.create(emitter -> {
            CachedProjectsMap cache = CachedProjectsMap.load().blockingGet();
            Set<ProjectCoordinate> resolved = new HashSet<>();

            Map<ProjectCoordinate, Map<Version<?>, Path>> projects = cache.getProjects();

            unresolvedDependencies.forEach((coordinate, version) -> {
                Map<Version<?>, Path> versions = projects.computeIfAbsent(
                        coordinate, coord -> new HashMap<>());

                AtomicReference<Path> path = new AtomicReference<>();
                if (versions.containsKey(version)) {
                    path.set(versions.get(version));
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

            resolved.forEach(unresolvedDependencies::remove);
            emitter.onSuccess(unresolvedDependencies);
        });
    }

    // TODO i don't like the side-effect-ness of this. However, it is important that the map is updated since it is
    //  a reference of the cache used for all resolved dependencies
    private Maybe<Path> retrieveAndCache(ProjectCoordinate coordinate, Version version) {
        return Maybe.create(emitter -> {
            logger.info("Attempting to retrieve {} {} from the registry.", coordinate, version);

            Optional<AuthToken> authToken = Optional.ofNullable(
                    authService.getStoredToken(context.getRc().getRepositoryUri())
                            .blockingGet());
            // Not all GETs may need to be authenticated, but in case it is a private org/package, this is necessary.
            HttpClient client = authToken
                    .map(HttpUtils::createHttpClientWithToken)
                    .orElseGet(HttpUtils::createHttpClient);
            // TODO handle uri composition more cleanly
            URI uri = UriUtils.appendPathSegments(
                    context.getRc().getRepositoryUri(),
                    coordinate.getOrganizationId(),
                    coordinate.getProjectId(),
                    "-",
                    RegistryUtils.createTarballName(coordinate, version));
            HttpGet get = new HttpGet(uri);
            HttpResponse response = client.execute(get);

            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                logger.error("Failed to retrieve package.");
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
}
