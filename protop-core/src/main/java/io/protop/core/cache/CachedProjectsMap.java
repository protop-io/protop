package io.protop.core.cache;

import io.protop.core.error.ServiceException;
import io.protop.core.logs.Logger;
import io.protop.core.manifest.ProjectCoordinate;
import io.protop.core.manifest.ProjectVersionBuilder;
import io.protop.core.storage.Storage;
import io.protop.version.InvalidVersionString;
import io.protop.version.Version;
import io.reactivex.Single;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CachedProjectsMap {

    private static final Logger logger = Logger.getLogger(CachedProjectsMap.class);

    private final Map<ProjectCoordinate, Map<Version<?>, Path>> projects;

    public static Single<CachedProjectsMap> load() {
        Path cacheDirectory = Storage.pathOf(Storage.GlobalDirectory.CACHE);

        // This is mutable so we can update it as we cache new dependencies.
        Map<ProjectCoordinate, Map<Version<?>, Path>> projects = new HashMap<>();

        return Single.fromCallable(() -> {
            Files.list(cacheDirectory).forEach(p -> memoizeProjects(projects, p));
            return new CachedProjectsMap(projects);
        });
    }

    private static void memoizeProjects(Map<ProjectCoordinate, Map<Version<?>, Path>> memo, Path path) {
        if (!Files.isDirectory(path)) {
            return;
        }

        File orgDir = path.toFile();
        String orgName = orgDir.getName();

        try {
            Files.list(path).forEach(projectDir -> {
                if (Files.isDirectory(projectDir)) {
                    ProjectCoordinate coordinate = new ProjectCoordinate(orgName, projectDir.toFile().getName());

                    Map<Version<?>, Path> versions = new HashMap<>();
                    try {
                        Files.list(projectDir).forEach(versionPath -> {
                            String fileName = versionPath.toFile().getName();
                            try {
                                Version<?> version = Version.valueOf(ProjectVersionBuilder.scheme, fileName);
                                versions.put(version, versionPath);
                            } catch (InvalidVersionString e) {
                                logger.debug("Not a valid version; skipping {}.", fileName);
                            }
                        });
                        memo.put(coordinate, versions);
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
        throw new ServiceException("Failed to load cached projects.", e);
    }
}
