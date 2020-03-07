package io.protop.core.cache;

import io.protop.core.logs.Logger;
import io.protop.core.manifest.ProjectCoordinate;
import io.protop.core.storage.Storage;
import io.protop.core.storage.StorageService;
import io.protop.version.Version;
import io.reactivex.Single;
import lombok.AllArgsConstructor;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
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
}
