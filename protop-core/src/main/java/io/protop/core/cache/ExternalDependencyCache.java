package io.protop.core.cache;

import io.protop.core.logs.Logger;
import io.protop.core.manifest.PackageId;
import io.protop.core.manifest.revision.InvalidVersionString;
import io.protop.core.manifest.revision.Version;
import io.protop.core.storage.Storage;
import io.reactivex.Single;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * May contain files directly retrieved from registries or symbolic links to projects
 * cached in the GitCache from git repositories.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ExternalDependencyCache {

    private static final Logger logger = Logger.getLogger(ExternalDependencyCache.class);

    private final Map<PackageId, Map<Version, Path>> projects;

    public static Single<ExternalDependencyCache> load() {
        Path cacheDirectory = Storage.pathOf(Storage.GlobalDirectory.CACHE);

        // This is mutable so we can update it as we cache new dependencies.
        Map<PackageId, Map<Version, Path>> projects = new HashMap<>();

        return Single.fromCallable(() -> {
            Files.list(cacheDirectory).forEach(p -> memoizeProjects(projects, p));
            return new ExternalDependencyCache(projects);
        });
    }

    private static void memoizeProjects(Map<PackageId, Map<Version, Path>> memo, Path path) {
        if (!Files.isDirectory(path)) {
            return;
        }

        File orgDir = path.toFile();
        String orgName = orgDir.getName();

        try {
            Files.list(path).forEach(projectDir -> {
                if (Files.isDirectory(projectDir)) {
                    PackageId packageId = new PackageId(orgName, projectDir.toFile().getName());

                    Map<Version, Path> revisions = new HashMap<>();
                    try {
                        Files.list(projectDir).forEach(revisionPath -> {
                            String fileName = revisionPath.toFile().getName();
                            try {
                                Version version = new Version(fileName);
                                revisions.put(version, revisionPath);
                            } catch (InvalidVersionString e) {
                                logger.warn("Not a valid revision; skipping {}.", fileName);
                            }
                        });
                        memo.put(packageId, revisions);
                    } catch (IOException e) {
                        handleError(e);
                    }
                }
            });
        } catch (IOException e) {
            handleError(e);
        }
    }

    private static void handleError(Throwable e) {
        throw new RuntimeException("Failed to load cached projects.", e);
    }
}
