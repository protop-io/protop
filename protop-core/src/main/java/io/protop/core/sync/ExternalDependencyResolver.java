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
import io.protop.core.manifest.revision.GitSource;
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
                Map<Coordinate, Map<GitSource, Map.Entry<Version, Path>>> preexistingGitCache = loadGitCache();
                Map<Coordinate, Map<Version, Path>> preexistingRegistryCache = loadVersionCache();
                Map<Coordinate, RevisionSource> aggregatedDependencies = aggregateDependencies(
                        projectDependencies, preexistingRegistryCache);

                Set<Coordinate> resolved = new HashSet<>();
                aggregatedDependencies.forEach((coordinate, revisionSource) -> {
                    try {
                        Map<GitSource, Map.Entry<Version, Path>> cachedGitRepos = preexistingGitCache.computeIfAbsent(coordinate, c -> new HashMap<>());
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
                        } else if (revisionSource instanceof GitSource) {
                            GitSource gitSource = (GitSource) revisionSource;

                            if (cachedGitRepos.containsKey(gitSource)) {
                                path.set(cachedGitRepos.get(gitSource).getValue());
                            } else {
                                // Everything should have already been cached for the git repo, because we can't retrieve a
                                // manifest otherwise.
                                // If that ever changes, we'll want to factor potentially retrieving from git into this called method.
                                retrieveFromGitCache(coordinate, gitSource).ifPresent(path::set);
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

    private Map<Coordinate, Map<GitSource, Map.Entry<Version, Path>>> loadGitCache() {
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

    private Map<Coordinate, Map<GitSource, Map.Entry<Version, Manifest>>> extractGitUrlManifestsFromCache(
            Map<Coordinate, Map<GitSource, Map.Entry<Version, Path>>> gitCache) {
        Map<Coordinate, Map<GitSource, Map.Entry<Version, Manifest>>> output = new HashMap<>();

        gitCache.forEach(((coordinate, gitUrlEntryMap) -> {
            Map<GitSource, Map.Entry<Version, Manifest>> gitManifestMap = new HashMap<>();
            gitUrlEntryMap.forEach(((gitSource, versionPathEntry) -> {
                Manifest manifest = Manifest.from(versionPathEntry.getValue())
                        .orElseThrow(ManifestNotFound::new);
                gitManifestMap.put(gitSource, Map.entry(versionPathEntry.getKey(), manifest));
            }));
            output.put(coordinate, gitManifestMap);
        }));

        return output;
    }

    private Map<Coordinate, RevisionSource> aggregateDependencies(
            Map<Coordinate, RevisionSource> projectDependencies,
            Map<Coordinate, Map<Version, Path>> registryCache) {

        Map<Coordinate, Map<Version, Manifest>> registryManifestsForReference =
                extractRegistryManifestsFromCache(registryCache);
        Map<Coordinate, Map.Entry<RevisionSource, Manifest>> aggregated = new HashMap<>();
        Queue<Map.Entry<Coordinate, RevisionSource>> unchecked =
                new LinkedList<>(projectDependencies.entrySet());

        while (!unchecked.isEmpty()) {
            Map.Entry<Coordinate, RevisionSource> entry = unchecked.poll();
            Coordinate coordinate = entry.getKey();
            RevisionSource revisionSource = entry.getValue();

            AtomicReference<Manifest> manifest = new AtomicReference<>();
            if (revisionSource instanceof Version) {
                Map<Version, Manifest> knownRegistryRevisions = registryManifestsForReference.computeIfAbsent(
                        coordinate, c -> new HashMap<>());
                Version version = (Version) revisionSource;
                if (knownRegistryRevisions.containsKey(version)) {
                    manifest.set(knownRegistryRevisions.get(version));
                } else {
                    // try to retrieve the manifest from the network
                    AggregatedManifest aggregatedManifest = retrieveAggregatedManifestFromRegistry(coordinate)
                            .blockingGet();
                    Map<Version, Manifest> aggregatedVersions = aggregatedManifest.getVersions();
                    registryManifestsForReference.put(coordinate, aggregatedVersions);
                    if (aggregatedVersions.containsKey(version)) {
                        manifest.set(aggregatedVersions.get(version));
                    }
                }
            } else if (revisionSource instanceof GitSource) {
                GitSource gitSource = (GitSource) revisionSource;
                Manifest gitRepoManifest = retrieveGitProjectManifest(coordinate, gitSource)
                        .blockingGet();
                manifest.set(gitRepoManifest);
            }

            Manifest resolvedManifest = manifest.get();
            if (Objects.isNull(resolvedManifest)) {
                throw new PackageNotFound(coordinate, revisionSource);
            } else {
                if (!aggregated.containsKey(coordinate) || (compare(aggregated.get(coordinate).getValue(), resolvedManifest) < 0)) {
                    aggregated.put(coordinate, Map.entry(revisionSource, resolvedManifest));
                    DependencyMap dependencyMap = resolvedManifest.getDependencies();
                    if (Objects.nonNull(dependencyMap)) {
                        unchecked.addAll(dependencyMap.getValues().entrySet());
                    }
                }
            }
        }

        logger.info("Aggregated {} external dependencies.", aggregated.size());

        // After aggregating the dependencies the first time, we need to clear out unused dependencies
        // because there may be extras left behind from the dependency map of a version less than the
        // the version that was ultimately required.
        Map<Coordinate, RevisionSource> reduced = new HashMap<>();
        Queue<Coordinate> reducible = new LinkedList<>(projectDependencies.keySet());

        while (!reducible.isEmpty()) {
            Coordinate coordinate = reducible.poll();
            Map.Entry<RevisionSource, Manifest> details = aggregated.get(coordinate);
            reduced.put(coordinate, details.getKey());
            DependencyMap dependencyMap = details.getValue().getDependencies();
            if (!Objects.isNull(dependencyMap)) {
                reducible.addAll(dependencyMap.getValues().keySet());
            }
        }

        logger.info("Reduced size of external dependencies to {}.", reduced.size());

        return reduced;
    }

    private int compare(Manifest a, Manifest b) {
        return a.getVersion().compareTo(b.getVersion());
    }

    private Maybe<AggregatedManifest> retrieveAggregatedManifestFromRegistry(Coordinate coordinate) {
        return Maybe.fromCallable(() -> {
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

    private Maybe<Manifest> retrieveGitProjectManifest(Coordinate coordinate, GitSource gitSource) {
        return Maybe.fromCallable(() -> {
            cacheService.unlock(Storage.GlobalDirectory.GIT_CACHE);

            Path orgPath = Storage.pathOf(Storage.GlobalDirectory.GIT_CACHE)
                    .resolve(coordinate.getOrganizationId());
            storageService.createDirectoryIfNotExists(orgPath);

            Path projectPath = orgPath.resolve(coordinate.getProjectId());
            storageService.createDirectoryIfNotExists(projectPath);

            Path gitUrlPath = projectPath.resolve(gitSource.getUrlEncoded());
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
                    logger.info("Retrieving {} {}.", coordinate, gitSource);
                    Git.cloneRepository()
                            .setURI(gitSource.getRawUrl())
                            .setDirectory(gitUrlDirectory)
                            .setCloneAllBranches(true)
                            .call();
                } catch (GitAPIException e) {
                    // TODO rethrow?
                    String message = String.format("Failed to retrieve %s from %s.", coordinate, gitSource);
                    logger.error(message, e);
                }
            }


            Optional<String> branchName = Optional.ofNullable(gitSource.getBranchName());
            try {
                if (branchName.isPresent()) {
                    logger.info("git directory: " + gitUrlDirectory);
                    Git git = Git.open(gitUrlDirectory);
                    git.checkout()
                            .setCreateBranch(false)
                            .setName(branchName.get())
                            .call();
                }
            } catch (GitAPIException e) {
                String message = String.format("Failed to find branch \"%s\" in Git repo for %s: %s.",
                        branchName.get(), coordinate, gitSource);
                logger.error(message, e);
            }

            cacheService.lock(Storage.GlobalDirectory.GIT_CACHE);

            return Manifest.from(gitUrlPath).orElse(null);
        });
    }

    private Optional<Path> retrieveFromRegistryAndCache(Coordinate coordinate, Version version) throws IOException, URISyntaxException {
        URI uri = RegistryUtils.createTarballUri(
                getRegistryUri(), coordinate, version);
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

    private Optional<Path> retrieveFromGitCache(Coordinate coordinate, GitSource gitSource) {
        GitCache gitCache = GitCache.load().blockingGet();

        Map<Coordinate, Map<GitSource, Map.Entry<Version, Path>>> projects = gitCache.getProjects();

        if (projects.containsKey(coordinate)) {
            Map<GitSource, Map.Entry<Version, Path>> revisions = projects.get(coordinate);
            if (revisions.containsKey(gitSource)) {
                return Optional.of(revisions.get(gitSource).getValue());
            }
        }

        logger.info("Project {} from {} not found in cache.", coordinate, gitSource);
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
