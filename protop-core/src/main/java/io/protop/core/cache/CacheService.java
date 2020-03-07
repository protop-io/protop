package io.protop.core.cache;

import io.protop.core.logs.Logger;
import io.protop.core.manifest.ProjectCoordinate;
import io.protop.core.storage.Storage;
import io.protop.core.storage.StorageService;
import io.protop.version.Version;
import io.reactivex.Single;
import lombok.AllArgsConstructor;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

@AllArgsConstructor
public class CacheService {

    private static final Logger logger = Logger.getLogger(CacheService.class);

    private final StorageService storageService;

    /**
     * Cache g-zipped response.
     */
    public Single<Path> cache(ProjectCoordinate coordinate, Version version, InputStream tarball) {
        return Single.create(emitter -> {
            logger.info("Caching {}.", coordinate);
            try {
                Path versionPath = resolveVersionPath(coordinate, version);
                unlock(versionPath);

                // write the tarball to version directory
                GZIPInputStream gis = new GZIPInputStream(tarball);
                TarArchiveInputStream tis = new TarArchiveInputStream(gis);

                TarArchiveEntry entry = tis.getNextTarEntry();
                logger.info("First entry: ", entry);
                while (Objects.nonNull(entry)) {
                    Path path = versionPath.resolve(entry.getName());
                    logger.info("Writing {}.", path.getFileName());
                    if (entry.isDirectory()) {
                        continue;
                    } else {
                        Files.createFile(path);
                        IOUtils.copy(tis, new FileOutputStream(path.toFile()));
                    }
                    entry = tis.getNextTarEntry();
                }
                tis.close();

                lock(versionPath);
                emitter.onSuccess(versionPath);
            } catch (Throwable t) {
                emitter.onError(t);
            }
        });
    }

    private Path resolveVersionPath(ProjectCoordinate coordinate, Version version) throws IOException {
        Path cache = Storage.pathOf(Storage.GlobalDirectory.CACHE);

        Path orgPath = cache.resolve(coordinate.getOrganizationId());
        if (!Files.isDirectory(orgPath)) {
            if (Files.exists(orgPath)) {
                Files.delete(orgPath);
            }
            Files.createDirectory(orgPath);
        }

        Path projectPath = orgPath.resolve(coordinate.getProjectId());
        if (!Files.isDirectory(projectPath)) {
            if (Files.exists(projectPath)) {
                Files.delete(projectPath);
            }
            Files.createDirectory(projectPath);
        }

        Path versionPath = projectPath.resolve(version.toString());
        if (!Files.isDirectory(versionPath)) {
            if (Files.exists(versionPath)) {
                Files.delete(versionPath);
            }
            Files.createDirectory(versionPath);
        }
        return versionPath;
    }

    private void unlock(Path dependencyDir) {
        logger.info("Unlocking dependencies.");
        walkAndApply(dependencyDir, file -> file.setWritable(true));
    }

    /**
     * Makes the directory and everything in it un-writable, mainly to protect against accidental modifications.
     */
    private void lock(Path dependencyDir) {
        walkAndApply(dependencyDir, File::setReadOnly);
    }

    private void walkAndApply(Path directory, Consumer<File> consumer) {
        File file = directory.toFile();
        consumer.accept(file);
        try {
            Files.walk(directory).forEach(child -> {
                consumer.accept(child.toFile());
            });
        } catch (IOException e) {
            logger.warn("Failed to walk file tree / to apply changes.", e);
        }
    }
}
