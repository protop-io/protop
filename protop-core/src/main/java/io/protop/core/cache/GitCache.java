package io.protop.core.cache;

import io.protop.core.logs.Logger;
import io.protop.core.manifest.Coordinate;
import io.protop.core.manifest.Manifest;
import io.protop.core.manifest.ManifestNotFound;
import io.protop.core.manifest.revision.GitUrl;
import io.protop.core.manifest.revision.InvalidRevision;
import io.protop.core.manifest.revision.Version;
import io.protop.core.storage.Storage;
import io.reactivex.Single;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public class GitCache {

    private static final Logger logger = Logger.getLogger(GitCache.class);

    private final Map<Coordinate, Map<GitUrl, Map.Entry<Version, Path>>> projects;

    public static Single<GitCache> load() {
        Path cacheDirectory = Storage.pathOf(Storage.GlobalDirectory.GIT_CACHE);

        // This is mutable so we can update it as we cache new dependencies.
        Map<Coordinate, Map<GitUrl, Map.Entry<Version, Path>>> projects = new HashMap<>();

        return Single.fromCallable(() -> {
            Files.list(cacheDirectory).forEach(p -> memoizeProjects(projects, p));
            return new GitCache(projects);
        });
    }

    private static void memoizeProjects(Map<Coordinate, Map<GitUrl, Map.Entry<Version, Path>>> memo, Path path) {
        if (!Files.isDirectory(path)) {
            return;
        }

        File orgDir = path.toFile();
        String orgName = orgDir.getName();

        try {
            Files.list(path).forEach(projectDir -> {
                if (Files.isDirectory(projectDir)) {
                    Coordinate coordinate = new Coordinate(orgName, projectDir.toFile().getName());

                    Map<GitUrl, Map.Entry<Version, Path>> revisions = new HashMap<>();
                    try {
                        Files.list(projectDir).forEach(gitUrlPath -> {
                            String fileName = gitUrlPath.toFile().getName();
                            try {
                                GitUrl gitUrl = GitUrl.fromEncodedUrl(fileName);
                                Manifest manifest = Manifest.from(gitUrlPath)
                                        .orElseThrow(ManifestNotFound::new);
                                revisions.put(gitUrl, Map.entry(manifest.getVersion(), gitUrlPath));
                            } catch (InvalidRevision e) {
                                logger.warn("Not a valid git repository URL; skipping {}.", fileName);
                            } catch (ManifestNotFound e) {
                                logger.warn("Manifest not found in supposed git repo; skipping {}.", fileName);
                            }
                        });
                        memo.put(coordinate, revisions);
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
