package io.protop.core.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.protop.core.Environment;
import io.protop.core.logs.Logger;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class StorageService {

    private static final Logger logger = Logger.getLogger(StorageService.class);

    private final ObjectMapper objectMapper;

    public StorageService() {
        this.objectMapper = Environment.getInstance().getObjectMapper();
    }

    public <T> void storeJson(T bean, Path path) {
        try {
            String fileString = objectMapper.writeValueAsString(bean);
            Files.write(path, fileString.getBytes());
        } catch (IOException e) {
            String message = "Failed to write file.";
            logger.error(message, e);
            throw new StorageException(message, e);
        }
    }

    public <T> Maybe<T> loadResource(Path resourcePath, Class<T> clazz) {
        if (!Files.exists(resourcePath)) {
            return Maybe.empty();
        }

        try {
            T obj = objectMapper.readValue(resourcePath.toFile(), clazz);
            return Maybe.just(obj);
        } catch (IOException e) {
            String message = "Failed to read file.";
            logger.error(message, e);
            throw new StorageException(message, e);
        }
    }

    public Completable createDirectoryIfNotExists(Path path) {
        return Completable.fromCallable(() -> {
            if (!Files.isDirectory(path)) {
                if (Files.isRegularFile(path)) {
                    throw new StorageException( "Cannot create directory because a file already exists.");
                }
                Files.createDirectory(path);
            }
            return null;
        });
    }

    public Single<Path> createTemporaryDirectory() {
        return Single.create(emitter -> {
            try {
                Path tempStoragePath = Storage.pathOf(Storage.GlobalDirectory.TEMP_PUBLICATION_CACHE);
                Path tempDirectory = Files.createTempDirectory(tempStoragePath, "idkk");
                tempDirectory.toFile().deleteOnExit();
                emitter.onSuccess(tempDirectory);
            } catch (Throwable t) {
                emitter.onError(t);
            }
        });
    }
}
