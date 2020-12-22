package io.protop.core.sync;

import io.grpc.Channel;
import io.grpc.stub.StreamObserver;
import io.protop.core.Context;
import io.protop.core.auth.AuthService;
import io.protop.core.cache.CacheService;
import io.protop.core.cache.ExternalDependencyCache;
import io.protop.core.cache.GitCache;
import io.protop.core.error.PackageNotFound;
import io.protop.core.grpc.AuthTokenCallCredentials;
import io.protop.core.grpc.GrpcService;
import io.protop.core.logs.Logger;
import io.protop.core.manifest.DependencyMap;
import io.protop.core.manifest.Manifest;
import io.protop.core.manifest.ManifestNotFound;
import io.protop.core.manifest.PackageId;
import io.protop.core.manifest.revision.GitSource;
import io.protop.core.manifest.revision.RevisionSource;
import io.protop.core.manifest.revision.Version;
import io.protop.core.storage.Storage;
import io.protop.core.storage.StorageService;
import io.protop.registry.domain.Package;
import io.protop.registry.services.RetrievalServiceGrpc;
import io.protop.registry.services.Retrieve;
import io.reactivex.Maybe;
import io.reactivex.Single;
import lombok.AllArgsConstructor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
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

    private final AuthService authService;
    private final StorageService storageService;
    private final CacheService cacheService;
    private final Context context;
    private final GrpcService grpcService;

    @Override
    public String getShortDescription() {
        return "external dependencies";
    }

    @Override
    public Single<Map<PackageId, RevisionSource>> resolve(
            Path dependencyDir, Map<PackageId, RevisionSource> projectDependencies) {

        if (projectDependencies.isEmpty()) {
            return Single.just(projectDependencies);
        }

        return Single.create(emitter -> {
            try {
                Map<PackageId, Map<GitSource, Map.Entry<Version, Path>>> preexistingGitCache = loadGitCache();
                Map<PackageId, Map<Version, Path>> preexistingRegistryCache = loadVersionCache();
                Map<PackageId, RevisionSource> aggregatedDependencies = aggregateDependencies(
                        projectDependencies, preexistingRegistryCache);

                Set<PackageId> resolved = new HashSet<>();
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
                                retrieveFromRegistryAndCache(coordinate, version)
                                        .map(Optional::ofNullable)
                                        .blockingGet()
                                        .ifPresent(path::set);
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

    private Map<PackageId, Map<Version, Path>> loadVersionCache() {
        return ExternalDependencyCache.load().blockingGet()
                .getProjects();
    }

    private Map<PackageId, Map<GitSource, Map.Entry<Version, Path>>> loadGitCache() {
        return GitCache.load().blockingGet()
                .getProjects();
    }

    private Map<PackageId, Map<Version, Manifest>> extractRegistryManifestsFromCache(
            Map<PackageId, Map<Version, Path>> versionCache) {
        Map<PackageId, Map<Version, Manifest>> output = new HashMap<>();

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

    private Map<PackageId, Map<GitSource, Map.Entry<Version, Manifest>>> extractGitUrlManifestsFromCache(
            Map<PackageId, Map<GitSource, Map.Entry<Version, Path>>> gitCache) {
        Map<PackageId, Map<GitSource, Map.Entry<Version, Manifest>>> output = new HashMap<>();

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

    private Map<PackageId, RevisionSource> aggregateDependencies(
            Map<PackageId, RevisionSource> projectDependencies,
            Map<PackageId, Map<Version, Path>> registryCache) {

        Map<PackageId, Map<Version, Manifest>> registryManifestsForReference =
                extractRegistryManifestsFromCache(registryCache);
        Map<PackageId, Map.Entry<RevisionSource, Manifest>> aggregated = new HashMap<>();
        Queue<Map.Entry<PackageId, RevisionSource>> unchecked =
                new LinkedList<>(projectDependencies.entrySet());

        while (!unchecked.isEmpty()) {
            Map.Entry<PackageId, RevisionSource> entry = unchecked.poll();
            PackageId packageId = entry.getKey();
            RevisionSource revisionSource = entry.getValue();

            AtomicReference<Manifest> manifest = new AtomicReference<>();
            if (revisionSource instanceof Version) {
                Map<Version, Manifest> knownRegistryRevisions = registryManifestsForReference.computeIfAbsent(
                        packageId, c -> new HashMap<>());
                Version version = (Version) revisionSource;
                if (knownRegistryRevisions.containsKey(version)) {
                    manifest.set(knownRegistryRevisions.get(version));
                } else {
                    // try to retrieve the manifest from the network
                    retrieveManifest(packageId, version).map(Optional::ofNullable).blockingGet().ifPresent(found -> {
                        manifest.set(found);
                        knownRegistryRevisions.put(version, found);
                    });
                }
            } else if (revisionSource instanceof GitSource) {
                GitSource gitSource = (GitSource) revisionSource;
                Manifest gitRepoManifest = retrieveGitProjectManifest(packageId, gitSource)
                        .blockingGet();
                manifest.set(gitRepoManifest);
            }

            Manifest resolvedManifest = manifest.get();
            if (Objects.isNull(resolvedManifest)) {
                throw new PackageNotFound(packageId, revisionSource);
            } else {
                if (!aggregated.containsKey(packageId) || (compare(aggregated.get(packageId).getValue(), resolvedManifest) < 0)) {
                    aggregated.put(packageId, Map.entry(revisionSource, resolvedManifest));
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
        Map<PackageId, RevisionSource> reduced = new HashMap<>();
        Queue<PackageId> reducible = new LinkedList<>(projectDependencies.keySet());

        while (!reducible.isEmpty()) {
            PackageId packageId = reducible.poll();
            Map.Entry<RevisionSource, Manifest> details = aggregated.get(packageId);
            reduced.put(packageId, details.getKey());
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

    private URL getRegistryUrl() throws MalformedURLException {
        return new URL(Optional.ofNullable(context.getRc().getRepositoryUrl()).orElse(""));
    }

    private RetrievalServiceGrpc.RetrievalServiceStub createRetrievalServiceStub() throws MalformedURLException {
        URL retrievalURL = getRegistryUrl();
        AuthTokenCallCredentials credentials = authService.getAuthTokenCallCredentials(retrievalURL);
        Channel channel = grpcService.getChannel(retrievalURL);
        // TODO skip call credentials if --no-auth flag passed
        return RetrievalServiceGrpc.newStub(channel)
                .withCallCredentials(credentials);
    }

    private Maybe<Manifest> retrieveManifest(PackageId packageId, Version version) {
        return Maybe.create(emitter -> {
            logger.info("Retrieving package manifest: {} {}", packageId, version);
            RetrievalServiceGrpc.RetrievalServiceStub retrievalServiceStub = createRetrievalServiceStub();

            retrievalServiceStub.retrieveMetadata(Retrieve.PackageQuery.newBuilder()
                    .setPackageId(packageId.toString())
                    .setVersion(version.toString())
                    .build(), new StreamObserver<>() {
                @Override
                public void onNext(Package.PackageMetadata value) {
                    logger.info("Received package metadata...");
                    emitter.onSuccess(Manifest.builder()
                            .organization(value.getOrganization())
                            .name(value.getProject())
                            .version(Version.of(value.getVersion()))
                            .dependencies(DependencyMap.from(value.getDependenciesList()))
                            .build());
                }

                @Override
                public void onError(Throwable t) {
                    emitter.onError(t);
                }

                @Override
                public void onCompleted() {
                    logger.info("Did not find this package's manifest in the registry.");
                    emitter.onComplete();
                }
            });
        });
    }

    private Maybe<Path> retrieveFromRegistryAndCache(PackageId packageId, Version version) throws IOException, URISyntaxException {
        return Maybe.create(emitter -> {
            logger.info("Retrieving package: {} {}", packageId, version);
            RetrievalServiceGrpc.RetrievalServiceStub retrievalServiceStub = createRetrievalServiceStub();

            AtomicReference<ByteArrayOutputStream> chunks = new AtomicReference<>(new ByteArrayOutputStream());

            retrievalServiceStub.retrieve(Retrieve.PackageQuery.newBuilder()
                    .setPackageId(packageId.toString())
                    .setVersion(version.toString())
                    .build(), new StreamObserver<>() {
                @Override
                public void onNext(io.protop.registry.data.Package.DataChunk value) {
                    try {
                        chunks.get().write(value.getData().toByteArray());
                    } catch (IOException e) {
                        // TODO wrap this exception?
                        emitter.onError(e);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    // TODO wrap this exception?
                    emitter.onError(t);
                }

                @Override
                public void onCompleted() {
                    cacheService.cacheFromRegistry(
                            packageId, version, new ByteArrayInputStream(chunks.get().toByteArray()))
                            .subscribe(emitter::onSuccess, emitter::onError)
                            .dispose();
                }
            });
        });
    }

    private Maybe<Manifest> retrieveGitProjectManifest(PackageId packageId, GitSource gitSource) {
        return Maybe.fromCallable(() -> {
            cacheService.unlock(Storage.GlobalDirectory.GIT_CACHE);

            Path orgPath = Storage.pathOf(Storage.GlobalDirectory.GIT_CACHE)
                    .resolve(packageId.getOrganization());
            storageService.createDirectoryIfNotExists(orgPath);

            Path projectPath = orgPath.resolve(packageId.getProject());
            storageService.createDirectoryIfNotExists(projectPath);

            Path gitUrlPath = projectPath.resolve(gitSource.getUrlEncoded());
            File gitUrlDirectory = gitUrlPath.toFile();

            boolean refreshGitSources = Optional.ofNullable(context.getRc().getRefreshGitSources())
                    .orElse(false);

            if (!gitUrlDirectory.exists() || refreshGitSources) {
                if (gitUrlDirectory.exists()) {
                    // TODO it might be nice to pull in changes if the directory exists,
                    //  rather than always wipe it out and then clone
                    final List<Path> pathsToDelete = Files.walk(gitUrlPath)
                            .sorted(Comparator.reverseOrder())
                            .collect(Collectors.toList());
                    for (Path path : pathsToDelete) {
                        Files.deleteIfExists(path);
                    }
                }

                try {
                    logger.info("Retrieving {} {}.", packageId, gitSource);
                    Git.cloneRepository()
                            .setURI(gitSource.getRawUrl())
                            .setDirectory(gitUrlDirectory)
                            .setCloneAllBranches(true)
                            .call();
                } catch (GitAPIException e) {
                    // TODO rethrow?
                    String message = String.format("Failed to retrieve %s from %s.", packageId, gitSource);
                    logger.error(message, e);
                }
            }


            Optional<String> branchName = Optional.ofNullable(gitSource.getBranchName());
            try {
                if (branchName.isPresent()) {
                    Git git = Git.open(gitUrlDirectory);
                    git.checkout()
                            .setCreateBranch(false)
                            .setName(branchName.get())
                            .call();
                }
            } catch (GitAPIException e) {
                String message = String.format("Failed to find branch \"%s\" in Git repo for %s: %s.",
                        branchName.get(), packageId, gitSource);
                logger.error(message, e);
            }

            cacheService.lock(Storage.GlobalDirectory.GIT_CACHE);

            return Manifest.from(gitUrlPath).orElse(null);
        });
    }

    private Optional<Path> retrieveFromGitCache(PackageId packageId, GitSource gitSource) {
        GitCache gitCache = GitCache.load().blockingGet();

        Map<PackageId, Map<GitSource, Map.Entry<Version, Path>>> projects = gitCache.getProjects();

        if (projects.containsKey(packageId)) {
            Map<GitSource, Map.Entry<Version, Path>> revisions = projects.get(packageId);
            if (revisions.containsKey(gitSource)) {
                return Optional.of(revisions.get(gitSource).getValue());
            }
        }

        logger.info("Project {} from {} not found in cache.", packageId, gitSource);
        return Optional.empty();
    }
}
