package io.protop.core.sync;

import io.protop.core.manifest.PackageId;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SyncUtils {

    private SyncUtils() {
        // no op
    }

    public static void createSymbolicLink(Path dependencyDir, PackageId packageId, Path srcDir) throws IOException {
        Path orgPath = dependencyDir.resolve(packageId.getOrganization());

        if (!Files.isDirectory(orgPath)) {
            Files.deleteIfExists(orgPath);
            Files.createDirectory(orgPath);
        }

        Path projectPath = orgPath.resolve(packageId.getProject());
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
