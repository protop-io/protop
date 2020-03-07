package io.protop.core.sync;

import io.protop.core.manifest.ProjectCoordinate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SyncUtils {

    private SyncUtils() {
        // no op
    }

    public static void createSymbolicLink(Path dependencyDir, ProjectCoordinate coordinate, Path srcDir) throws IOException {
        Path orgPath = dependencyDir.resolve(coordinate.getOrganizationId());

        if (!Files.isDirectory(orgPath)) {
            Files.deleteIfExists(orgPath);
            Files.createDirectory(orgPath);
        }

        Path projectPath = orgPath.resolve(coordinate.getProjectId());
        if (Files.exists(projectPath)) {
            if (Files.isSymbolicLink(projectPath)) {
                return;
            } else {
                Files.delete(projectPath);
            }
        }

        Files.createSymbolicLink(projectPath, srcDir);
    }
}
