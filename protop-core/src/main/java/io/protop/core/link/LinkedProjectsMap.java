package io.protop.core.link;

import io.protop.core.error.ServiceException;
import io.protop.core.logs.Logger;
import io.protop.core.manifest.ProjectCoordinate;
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
 * Represents all dependencies that are currently linked on the machine.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LinkedProjectsMap {

    private static final Logger logger = Logger.getLogger(LinkedProjectsMap.class);

    private final Map<ProjectCoordinate, Path> projects;

    public static Single<LinkedProjectsMap> load() {
        Path linksDirectory = Storage.pathOf(Storage.GlobalDirectory.LINKS);
        Map<ProjectCoordinate, Path> projects = new HashMap<>();

        return Single.fromCallable(() -> {
            Files.list(linksDirectory).forEach(p -> memoizeProjects(projects, p));
            return new LinkedProjectsMap(projects);
        });
    }

    private static void memoizeProjects(Map<ProjectCoordinate, Path> memo, Path path) {
        if (!Files.isDirectory(path)) {
            return;
        }

        File org = path.toFile();
        String orgName = org.getName();

        try {
            Files.list(path).forEach(p -> {
                if (Files.isSymbolicLink(p)) {
                    ProjectCoordinate name = new ProjectCoordinate(orgName, p.toFile().getName());
                    memo.put(name, p);
                }
            });
        } catch (IOException e) {
            throw new ServiceException("Failed to load linked projects.");
        }
    }
}
