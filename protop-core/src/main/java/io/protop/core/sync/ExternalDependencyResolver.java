package io.protop.core.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.protop.core.Context;
import io.protop.core.Environment;
import io.protop.core.auth.AuthService;
import io.protop.core.cache.CacheService;
import io.protop.core.cache.ExternalDependencyCache;
import io.protop.core.cache.GitCache;
import io.protop.core.error.PackageNotFound;
import io.protop.core.logs.Logger;
import io.protop.core.manifest.*;
import io.protop.core.manifest.revision.GitUrl;
import io.protop.core.manifest.revision.RevisionSource;
import io.protop.core.manifest.revision.Version;
import io.protop.core.storage.Storage;
import io.protop.core.storage.StorageService;
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
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Resolves dependencies if they have been published to a registry.
 *
 * If a dependency is pointed at a git repo in the current project, it will be resolved from that repo.
 * However, if any dependency itself requires that project,
 * then the version originally pulled from the git repo is automatically invalidated IF the third party dependency
 * is found to be a greater version (whether it comes from a registry or a git repo itself).
 */
@AllArgsConstructor
public class ExternalDependencyResolver implements DependencyResolver {

    private static final Logger logger = Logger.getLogger(ExternalDependencyResolver.class);

    private final AuthService<?> authService;
    private final StorageService storageService;
    private final CacheService cacheService;
    private final Context context;

    @Override
    public String getShortDescription() {
        return "external dependencies";
    }

    @Override
    public Single<Map<Coordinate, RevisionSource>> resolve(
            Path dependencyDir, Map<Coordinate, RevisionSource> projectDependencies) {

        if (projectDependencies.isEmpty()) {
            return Single.just(projectDependencies);
        }

        return Single.create(emitter -> {
            try {
                Map<Coordinate, Map<GitUrl, Map.Entry<Version, Path>>> preexistingGitCache = loadGitCache();
                Map<Coordinate, Map<Version, Path>> preexistingRegistryCache = loadVersionCache();

                Map<Coordinate, RevisionSource> aggregatedDependencies = aggregateDependencies(
                        projectDependencies,
                        preexistingGitCache,
                        preexistingRegistryCache);

                logger.info("Aggregated dependencies: " + new ObjectMapper().writeValueAsString(aggregatedDependencies));
                // TODO After aggregating the dependencies the first time, we need to walk through the tree again to clear out unused
                //  dependencies. There maybe be extras left behind from the dependency map of a version of a coordinate less than the
                //  the version that was ultimately required.

                Set<Coordinate> resolved = new HashSet<>();
                aggregatedDependencies.forEach((coordinate, revisionSource) -> {
                    try {
                        Map<GitUrl, Map.Entry<Version, Path>> cachedGitRepos = preexistingGitCache.computeIfAbsent(coordinate, c -> new HashMap<>());
                        Map<Version, Path> cachedVersions = preexistingRegistryCache.computeIfAbsent(coordinate, c -> new HashMap<>());

                        AtomicReference<Path> path = new AtomicReference<>();
                        if (revisionSource instanceof Version) {
                            Version version = (Version) revisionSource;

                            if (cachedVersions.containsKey(version)) {
                                path.set(cachedVersions.get(version));
                            } else {
                                logger.info("Not found; attempting to retrieve from registry.");
                                retrieveFromRegistryAndCache(coordinate, version).ifPresent(path::set);
                            }
                        } else if (revisionSource instanceof GitUrl) {
                            GitUrl gitUrl = (GitUrl) revisionSource;

                            if (cachedGitRepos.containsKey(gitUrl)) {
                                path.set(cachedGitRepos.get(gitUrl).getValue());
                            } else {
                                // Everything should have already been cached for the git repo, because we can't retrieve a
                                // manifest otherwise.
                                // If that ever changes, we'll want to factor potentially retrieving from git into this called method.
                                retrieveFromGitCache(coordinate, gitUrl).ifPresent(path::set);
                            }
                        }

                        Path sourceDir = path.get();
                        if (Objects.nonNull(sourceDir)) {
                            try {
                                SyncUtils.createSymbolicLink(dependencyDir, coordinate, sourceDir);
                                resolved.add(coordinate);
                            } catch (IOException e) {
                                logger.error("Could not create link to cached dependency.", e);
                                throw new IncompleteSync();
                            }
                        }
                    } catch (Throwable t) {
                        emitter.onError(t);
                    }
                });

                resolved.forEach(aggregatedDependencies::remove);

                emitter.onSuccess(aggregatedDependencies);
            } catch (Throwable t) {
                emitter.onError(t);
            }
        });
    }

    private Map<Coordinate, Map<Version, Path>> loadVersionCache() {
        return ExternalDependencyCache.load().blockingGet()
                .getProjects();
    }

    private Map<Coordinate, Map<GitUrl, Map.Entry<Version, Path>>> loadGitCache() {
        return GitCache.load().blockingGet()
                .getProjects();
    }

    private Map<Coordinate, Map<Version, Manifest>> extractRegistryManifestsFromCache(
            Map<Coordinate, Map<Version, Path>> versionCache) {
        Map<Coordinate, Map<Version, Manifest>> output = new HashMap<>();

        versionCache.forEach((coordinate, versionPathMap) -> {
            Map<Version, Manifest> versionManifestMap = new HashMap<>();
            versionPathMap.forEach((version, path) -> {
                Manifest manifest = Manifest.from(path)
                        .orElseThrow(ManifestNotFound::new);
                versionManifestMap.put(version, manifest);
            });
            output.put(coordinate, versionManifestMap);
        });

        return output;
    }

    private Map<Coordinate, Map<GitUrl, Map.Entry<Version, Manifest>>> extractGitUrlManifestsFromCache(
            Map<Coordinate, Map<GitUrl, Map.Entry<Version, Path>>> gitCache) {
        Map<Coordinate, Map<GitUrl, Map.Entry<Version, Manifest>>> output = new HashMap<>();

        gitCache.forEach(((coordinate, gitUrlEntryMap) -> {
            Map<GitUrl, Map.Entry<Version, Manifest>> gitManifestMap = new HashMap<>();
            gitUrlEntryMap.forEach(((gitUrl, versionPathEntry) -> {
                Manifest manifest = Manifest.from(versionPathEntry.getValue())
                        .orElseThrow(ManifestNotFound::new);
                gitManifestMap.put(gitUrl, Map.entry(versionPathEntry.getKey(), manifest));
            }));
            output.put(coordinate, gitManifestMap);
        }));

        return output;
    }

    private Map<Coordinate, RevisionSource> aggregateDependencies(
            Map<Coordinate, RevisionSource> projectDependencies,
            Map<Coordinate, Map<GitUrl, Map.Entry<Version, Path>>> gitCache,
            Map<Coordinate, Map<Version, Path>> registryCache) {

        Map<Coordinate, Map<GitUrl, Map.Entry<Version, Manifest>>> gitManifestsForReference =
                extractGitUrlManifestsFromCache(gitCache);
        Map<Coordinate, Map<Version, Manifest>> registryManifestsForReference =
                extractRegistryManifestsFromCache(registryCache);

        Map<Coordinate, RevisionSource> aggregated = new HashMap<>();

        // We make two separate queues so that we can treat them slightly differently.
        // Mainly, throw an exception if a third party dependency has a girurl version.
        Queue<Map.Entry<Coordinate, RevisionSource>> originalUnchecked =
                new LinkedList<>(projectDependencies.entrySet());
        Queue<Map.Entry<Coordinate, RevisionSource>> thirdPartyUnchecked =
                new LinkedList<>();

        while (!originalUnchecked.isEmpty() || !thirdPartyUnchecked.isEmpty()) {
            boolean originalDependency = !originalUnchecked.isEmpty();
            Map.Entry<Coordinate, RevisionSource> entry = originalDependency
                    ? originalUnchecked.poll()
                    : thirdPartyUnchecked.poll();
            Coordinate coordinate = entry.getKey();
            RevisionSource revisionSource = entry.getValue();

            logger.info("checking dependency: {} {}.", coordinate, revisionSource);

            Map<Version, Manifest> knownVersions = registryManifestsForReference.computeIfAbsent(
                    coordinate, c -> new HashMap<>());
//            Map<GitUrl, Manifest> knownGitRepos = TODO???

            AtomicReference<Manifest> manifest = new AtomicReference<>();

            if (revisionSource instanceof Version) {
                if (knownVersions.containsKey(revisionSource)) {
                    manifest.set(knownVersions.get(revisionSource));
                } else {
                    // try to retrieve the manifest from the network
                    AggregatedManifest aggregatedManifest = retrieveAggregatedManifestFromRegistry(coordinate)
                            .blockingGet();
                    Map<Version, Manifest> aggregatedVersions = aggregatedManifest.getVersions();
                    registryManifestsForReference.put(coordinate, aggregatedVersions);
                    if (aggregatedVersions.containsKey(revisionSource)) {
                        manifest.set(aggregatedVersions.get(revisionSource));
                    }
                }
            } else if (revisionSource instanceof GitUrl) {
                // Retrieve and cache. We have to do this now so that we can continue to build the dependency tree.
                Manifest gitRepoManifest = retrieveGitProjectManifest(coordinate, (GitUrl) revisionSource)
                        .blockingGet();
                manifest.set(gitRepoManifest);

                // TODO - we need to update the git cache now to reflect the addition, right?
            }

            Manifest resolvedManifest = manifest.get();

            if (Objects.isNull(resolvedManifest)) {
                throw new PackageNotFound(coordinate, revisionSource);
            } else {
                if (!aggregated.containsKey(coordinate) || (compare(aggregated.get(coordinate), revisionSource) < 0)) {
                    aggregated.put(coordinate, revisionSource);
                    DependencyMap dependencyMap = resolvedManifest.getDependencies();

                    if (Objects.nonNull(dependencyMap)) {
                        dependencyMap.getValues().forEach((subDependencyCoordinate, subDependencyVersion) -> {
                            if (!aggregated.containsKey(subDependencyCoordinate)
                                    || compare(aggregated.get(subDependencyCoordinate), subDependencyVersion) < 0) {

                                thirdPartyUnchecked.add(new HashMap.SimpleImmutableEntry<>(
                                        subDependencyCoordinate, subDependencyVersion));
                            }
                        });
                    }
                }
            }
        }

        return aggregated;
    }

    // 1 = a is greater, -1 = b is greater, 0 = both are equal.
    private int compare(RevisionSource a, RevisionSource b) {
        if (a instanceof Version) {
            if (b instanceof Version) {
                return ((Version) a).compareTo((Version) b);
            } else {
                return -1;
            }
        } else {
            if (b instanceof Version) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    private Single<AggregatedManifest> retrieveAggregatedManifestFromRegistry(Coordinate coordinate) {
        return Single.fromCallable(() -> {
            URI uri = RegistryUtils.createManifestUri(getRegistryUri(), coordinate);
            HttpResponse response = createHttpClient()
                    .execute(new HttpGet(uri));

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                logger.error("Did not find project in registry: " + coordinate);
                // TODO handle different responses better
            } else {
                try {
                    ObjectMapper objectMapper = Environment.getInstance().getObjectMapper();
                    String stringEntity = EntityUtils.toString(response.getEntity());
                    return objectMapper.readValue(stringEntity, AggregatedManifest.class);
                } catch (Exception e) {
                    logger.error("Failed to parse manifest for " + coordinate, e);
                }
            }

            return null;
        });
    }

    private Maybe<Manifest> retrieveGitProjectManifest(Coordinate coordinate, GitUrl gitUrl) {
        return Maybe.fromCallable(() -> {
            Path orgPath = Storage.pathOf(Storage.GlobalDirectory.GIT_CACHE)
                    .resolve(coordinate.getOrganizationId());
            storageService.createDirectoryIfNotExists(orgPath);

            Path projectPath = orgPath.resolve(coordinate.getProjectId());
            storageService.createDirectoryIfNotExists(projectPath);

            Path gitUrlPath = projectPath.resolve(gitUrl.getUrlEncoded());
            File gitUrlDirectory = gitUrlPath.toFile();

            boolean refreshGitSources = Optional.ofNullable(context.getRc().getRefreshGitSources())
                    .orElse(false);

            if (!gitUrlDirectory.exists() || refreshGitSources) {
                if (gitUrlDirectory.exists()) {
                    // TODO it might be nice to pull in changes if the directory exists, rather than always wipe it out and then clone
                    final List<Path> pathsToDelete = Files.walk(gitUrlPath)
                            .sorted(Comparator.reverseOrder())
                            .collect(Collectors.toList());
                    for (Path path : pathsToDelete) {
                        Files.deleteIfExists(path);
                    }
                }

                try {
                    logger.info("Retrieving {} {}.", coordinate, gitUrl);
                    Git.cloneRepository()
                            .setURI(gitUrl.getRaw())
                            .setDirectory(gitUrlDirectory)
                            .call();
                } catch (GitAPIException e) {
                    String message = String.format("Failed to retrieve %s from %s.", coordinate, gitUrl);
                    logger.error(message, e);
                }
            }

            return Manifest.from(gitUrlPath).orElse(null);
        });
    }

    private Optional<Path> retrieveFromRegistryAndCache(Coordinate coordinate, Version version) throws IOException, URISyntaxException {
        URI uri = RegistryUtils.createTarballUri(
                getRegistryUri(),
                coordinate,
                version);
        HttpResponse response = createHttpClient()
                .execute(new HttpGet(uri));

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != HttpStatus.SC_OK) {
            logger.error("Failed to retrieve package. URI: {}. Status code: {}.",
                    uri, statusCode);
            // TODO handle better, i.e. map to a specific exception based on the response/code from the registry.
            return Optional.empty();
        } else {
            Path path = cacheService.cacheFromRegistry(coordinate, version, response.getEntity().getContent())
                    .blockingGet();

            logger.info("Happy path achieved.");
            return Optional.of(path);
        }
    }

    private Optional<Path> retrieveFromGitCache(Coordinate coordinate, GitUrl gitUrl) {
        GitCache gitCache = GitCache.load().blockingGet();

        Map<Coordinate, Map<GitUrl, Map.Entry<Version, Path>>> projects = gitCache.getProjects();

        if (projects.containsKey(coordinate)) {
            Map<GitUrl, Map.Entry<Version, Path>> revisions = projects.get(coordinate);
            if (revisions.containsKey(gitUrl)) {
                return Optional.of(revisions.get(gitUrl).getValue());
            }
        }

        logger.info("Project {} from {} not found in cache.", coordinate, gitUrl);
        return Optional.empty();
    }

    @Nullable
    private URI getRegistryUri() {
        return context.getRc().getRepositoryUri();
    }

    private HttpClient createHttpClient() {
        return Optional.ofNullable(authService.getStoredToken(getRegistryUri()).blockingGet())
                .map(HttpUtils::createHttpClientWithToken)
                .orElseGet(HttpUtils::createHttpClient);
    }
}
