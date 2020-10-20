package io.protop.core.sync;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import io.protop.core.Context;
import io.protop.core.auth.AuthService;
import io.protop.core.cache.CacheService;
import io.protop.core.logs.Logger;
import io.protop.core.manifest.Coordinate;
import io.protop.core.manifest.DependencyMap;
import io.protop.core.manifest.Manifest;
import io.protop.core.manifest.revision.RevisionSource;
import io.protop.core.storage.Storage;
import io.protop.core.storage.StorageService;
import io.protop.core.sync.status.SyncStatus;
import io.protop.core.sync.status.Syncing;
import io.reactivex.Observable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class SyncService {

    private static final Logger logger = Logger.getLogger(SyncService.class);

    private final AuthService<?> authService;
    private final StorageService storageService;
    private final Context context;
    private final CacheService cacheService;

    public SyncService(AuthService<?> authService,
                       StorageService storageService,
                       Context context) {
        this.authService = authService;
        this.storageService = storageService;
        this.context = context;
        this.cacheService = new CacheService(storageService);
    }

    /**
     * Sync dependencies.
     * @param dependencyResolutionConfiguration dependency resolution details.
     * @return unresolved dependencies.
     */
    public Observable<SyncStatus> sync(DependencyResolutionConfiguration dependencyResolutionConfiguration) {
        return Observable.create(emitter -> {
            Path dependenciesDir = resolveEmptySubDir(Storage.ProjectDirectory.DEPS);

            List<DependencyResolver> resolvers = new ArrayList<>();
            if (dependencyResolutionConfiguration.includesLinkedDependencies) {
                resolvers.add(new LinkedDependencyResolver());
            }

            // Currently always use cached/external dependencies.
            resolvers.add(new ExternalDependencyResolver(authService, storageService, cacheService, context));

            DependencyMap dependencyMap = Optional.ofNullable(context.getManifest().getDependencies())
                    .orElseGet(DependencyMap::new);
            AtomicReference<Map<Coordinate, RevisionSource>> unresolvedDependencies = new AtomicReference<>(
                    dependencyMap.getValues());

            resolvers.forEach(resolver -> {
                emitter.onNext(new Syncing(resolver.getShortDescription()));
                Map<Coordinate, RevisionSource> next = resolver.resolve(
                        dependenciesDir,
                        unresolvedDependencies.get())
                        .blockingGet();
                unresolvedDependencies.set(next);
            });

            mergeDepsToPath(dependenciesDir);

            Map<Coordinate, RevisionSource> ultimatelyUnresolved = unresolvedDependencies.get();
            if (!ultimatelyUnresolved.isEmpty()) {
                emitter.onError(new IncompleteSync(ultimatelyUnresolved));
            } else {
                emitter.onComplete();
            }
        });
    }

    private List<File> getProtoFilesInDir(Path dir) {
        Collection<File> files = FileUtils.listFiles(
                dir.toFile(), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);

        return files.stream()
                // For now, assume this is the only extension. If it becomes necessary later, we can
                // explore allowing custom extensions/patterns to be provided.
                .filter(file -> file.getName().endsWith(".proto"))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @VisibleForTesting
    @EqualsAndHashCode
    @Getter
    static class DirectoryWithFiles {
        private final Map<String, FileWithRootDir> files;
        private final Map<String, DirectoryWithFiles> subdirectories;

        DirectoryWithFiles() {
            this(new HashMap<>(), new HashMap<>());
        }

        @VisibleForTesting
        DirectoryWithFiles(final Map<String, FileWithRootDir> files, final Map<String, DirectoryWithFiles> subdirectories) {
            this.files = files;
            this.subdirectories = subdirectories;
        }
    }

    private List<File> validProtoFilesOrDirectories(File[] files) {
        List<String> invalidDirectoryNames = ImmutableList.of("node_modules");
        List<File> validFiles = new ArrayList<>();
        for (File file : files) {
            final String fileName = file.getName();

            if (!fileName.startsWith(".") && !file.isHidden()) {
                if (file.isDirectory() && !invalidDirectoryNames.contains(fileName)) {
                    validFiles.add(file);
                } else if (file.isFile() && fileName.endsWith(".proto")) {
                    validFiles.add(file);
                }
            }
        }
        return validFiles;
    }

    /**
     * Filter the children directories and files and add to the given tree.
     */
    private void filterValidChildren(
            final DirectoryWithFiles directoryWithFiles,
            final File project) throws FileAlreadyExistsException {
        final Path rootDir = Manifest
                .from(project)
                .map(Manifest::getRootDir)
                .orElse(Path.of(""));

        filterValidChildren(directoryWithFiles, project, rootDir);
    }

    @VisibleForTesting
    void filterValidChildren(
            final DirectoryWithFiles directoryWithFiles,
            final File path,
            final Path rootDir) throws FileAlreadyExistsException {
        if (path.isDirectory()) {
            final File[] children = Optional.ofNullable(path.listFiles())
                    .orElse(new File[]{});

            for (File child : validProtoFilesOrDirectories(children)) {
                if (child.isDirectory()) {
                    DirectoryWithFiles childDirectory = directoryWithFiles.subdirectories.computeIfAbsent(
                            child.getName(),
                            name -> new DirectoryWithFiles());
                    filterValidChildren(childDirectory, child, rootDir);
                    // If the child directory ended up with nothing in it, remove it.
                    // This isn't the most efficient thing, but it does save us from having to traverse the
                    // entire tree again in order to remove empty directories.
                    if (childDirectory.subdirectories.isEmpty() && childDirectory.files.isEmpty()) {
                        directoryWithFiles.subdirectories.remove(child.getName());
                    }
                } else {
                    if (directoryWithFiles.files.containsKey(child.getName())) {
                        String message = String.format(
                                "Proto file with name %s already exists under parent directory \"%s\"",
                                child.getName(),
                                path.getName());
                        throw new FileAlreadyExistsException(message);
                    } else {
                        directoryWithFiles.files.put(child.getName(), new FileWithRootDir(rootDir, child));
                    }
                }
            }
        } else {
            throw new RuntimeException("Can not traverse this file since it is not a directory!");
        }
    }

    /**
     * Create subdirectories and symlink files in the given parent.
     */
    private static void mergeChildrenToParentDirectory(
            final Path protopPathDir, final Path parent, final DirectoryWithFiles children) throws IOException {
        for (Map.Entry<String, FileWithRootDir> fileEntry : children.getFiles().entrySet()) {
            final Path link = parent.resolve(fileEntry.getKey());

            fileEntry.getValue().createSymbolicLink(protopPathDir, link);
        }

        for (Map.Entry<String, DirectoryWithFiles> directoryEntry : children.getSubdirectories().entrySet()) {
            final Path subdirectory = parent.resolve(directoryEntry.getKey());

            mergeChildrenToParentDirectory(protopPathDir, subdirectory, directoryEntry.getValue());
        }
    }

    @VisibleForTesting
    void mergeDepsToPath(final Path depsDir) throws IOException {
        final DirectoryWithFiles depsTree = new DirectoryWithFiles();
        final File[] orgs = Optional
                .ofNullable(depsDir.toFile().listFiles())
                .orElse(new File[]{});

        for (File org : orgs) {
            if (org.isDirectory()) {
                final File[] projects = Optional
                        .ofNullable(org.listFiles())
                        .orElse(new File[]{});

                for (File project : projects) {
                    // For every project, traverse its items and add to the dependency tree.
                    if (project.isDirectory()) {
                        filterValidChildren(depsTree, project);
                    }
                }
            }
        }

        final Path mergedDepsPath = resolveEmptySubDir(Storage.ProjectDirectory.PATH);
        mergeChildrenToParentDirectory(mergedDepsPath, mergedDepsPath, depsTree);
    }

    /**
     * Generic method to resolve an empty subdirectory (create, or otherwise delete and recreate)
     * under the .protop directory.
     */
    private Path resolveEmptySubDir(Storage.ProjectDirectory dir) throws IOException {
        Path protopPath = context.getProjectLocation()
                .resolve(Storage.ProjectDirectory.PROTOP.getName());
        storageService.createDirectoryIfNotExists(protopPath)
                .blockingAwait();
        Path dirPath = protopPath.resolve(dir.getName());
        storageService.createDirectoryIfNotExists(dirPath)
                .blockingAwait();

        // This is fairly inexpensive and ensures better likelihood of the correct final state of things.
        FileUtils.cleanDirectory(dirPath.toFile());

        return dirPath;
    }
}
