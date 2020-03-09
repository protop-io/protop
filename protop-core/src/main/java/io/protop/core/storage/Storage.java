package io.protop.core.storage;

import io.protop.core.error.ServiceError;
import io.protop.core.error.ServiceException;
import io.protop.core.logs.Logger;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Storage {

    private Storage() {
        // no op
    }

    private static final Logger logger = Logger.getLogger(Storage.class);
    private static final String PROTOP_STORAGE_DIR_NAME = ".protop";
    private static final Path homePath = Paths.get(
            System.getProperty("user.home"), PROTOP_STORAGE_DIR_NAME);

    public static Path getHomePath() {
        return homePath;
    }

    /**
     * Returns full path of the resource directory.
     * @param resource - resource type.
     * @return
     */
    public static Path pathOf(GlobalDirectory resource) {
        Path subDirectory = homePath.resolve(resource.getName());
        logger.info("Resolving resource path: {}.", subDirectory.toAbsolutePath().toString());

        try {
            if (!Files.isDirectory(homePath)) {
                if (Files.isRegularFile(homePath)) {
                    throw new ServiceException(ServiceError.STORAGE_ERROR,
                            "Found a file where a directory (or nothing) was expected.");
                }
                logger.info("Initializing base directory: {}.", homePath.toAbsolutePath().toString());
                Files.createDirectory(homePath);
            }
            if (!Files.isDirectory(subDirectory)) {
                logger.info("Initializing resource directory: {}.", homePath.toAbsolutePath().toString());
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
        LINKS("links"),

        // External dependency cache.
        CACHE("cache");

        private final String name;
    }

    @Getter
    @AllArgsConstructor
    public enum ProjectDirectory {

        PROTOP("protop"),

        DEPS("deps");

        private final String name;
    }
}
