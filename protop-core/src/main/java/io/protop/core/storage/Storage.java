package io.protop.core.storage;

import io.protop.core.error.ServiceError;
import io.protop.core.error.ServiceException;
import io.protop.core.logs.Logger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class Storage {

    private static final Logger logger = Logger.getLogger(Storage.class);
    private static final String PROTOP_STORAGE_DIR_NAME = ".protop";
    private static final Path basePath = Paths.get(
            System.getProperty("user.home"), PROTOP_STORAGE_DIR_NAME);

    /**
     * Returns full path of the resource directory.
     * @param resource - resource type.
     * @return
     */
    public static Path pathOf(GlobalDirectory resource) {
        Path subDirectory = basePath.resolve(resource.getDirName());
        logger.info("Resolving resource path: {}.", subDirectory.toAbsolutePath().toString());

        try {
            if (!Files.isDirectory(basePath)) {
                if (Files.isRegularFile(basePath)) {
                    throw new ServiceException(ServiceError.STORAGE_ERROR,
                            "Found a file where a directory (or nothing) was expected.");
                }
                logger.info("Initializing base directory: {}.", basePath.toAbsolutePath().toString());
                Files.createDirectory(basePath);
            }
            if (!Files.isDirectory(subDirectory)) {
                logger.info("Initializing resource directory: {}.", basePath.toAbsolutePath().toString());
                Files.createDirectory(subDirectory);
            }
        } catch (IOException e) {
            String message = "Failed to load resource.";
            logger.error(message, e);
            throw new ServiceException(ServiceError.STORAGE_ERROR, message);
        }

        return subDirectory;
    }

    @Getter
    @AllArgsConstructor
    public enum GlobalDirectory {

        // CredentialStore storage.
        SESSION_STORE(".auth"),

        // Temporary storage for publication tarball.
        TEMP_PUBLICATION_CACHE(".tmp"),

        // Sym links to `link`ed projects.
        LINKS("links");

        private String dirName;
    }
}
