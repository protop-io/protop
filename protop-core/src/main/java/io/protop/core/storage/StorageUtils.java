package io.protop.core.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class StorageUtils {

    /**
     * Creates a directory at the given path if it doesn't already exist.
     * @param path
     */
    public static void createDirectoryIfNotExists(Path path) throws IOException {
        if (!Files.isDirectory(path)) {
            if (Files.isRegularFile(path)) {
                String message = String.format(
                        "Attempting to created a directory where a file already exists: %s.",
                        path.toString());
                throw new IOException(message);
            }
            Files.createDirectory(path);
        }
    }
}
