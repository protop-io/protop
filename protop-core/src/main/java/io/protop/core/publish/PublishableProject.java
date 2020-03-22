package io.protop.core.publish;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;
import io.protop.core.logs.Logger;
import io.protop.core.manifest.Manifest;
import io.protop.core.manifest.ManifestNotFound;
import io.protop.core.storage.Storage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class PublishableProject {

    private static final Logger logger = Logger.getLogger(PublishableProject.class);

    private static final String PROTO_FILE_EXT = "proto";
    private static final Set<String> publishableFileExtensions = ImmutableSet.of(
            PROTO_FILE_EXT);

    private static final String MANIFEST_NAME = "protop.json";
    private static final Set<String> publishableFileNames = ImmutableSet.of(
            MANIFEST_NAME);

    @NotNull
    private final Manifest manifest;

    @NotNull
    private final Path projectLocation;

    @NotNull
    private final Collection<File> files;

    public static PublishableProject from(Path projectLocation) {
        Manifest manifest = Manifest.from(projectLocation)
                .orElseThrow(ManifestNotFound::new);

        Collection<File> publishableFiles = getPublishableFiles(projectLocation);
        if (publishableFiles.isEmpty()) {
            throw new PublishFailed("No files found to publish.");
        } else if (publishableFiles.stream().noneMatch(PublishableProject::isProtoFile)) {
            throw new PublishFailed("No proto files found to publish");
        }

        return new PublishableProject(manifest, projectLocation, ImmutableList.copyOf(publishableFiles));
    }

    public CompressedArchiveDetails compressAndZip() {
        return compressAndZip(projectLocation);
    }

    public CompressedArchiveDetails compressAndZip(Path directory) {
        Path destination = Storage.pathOf(Storage.GlobalDirectory.TEMP_PUBLICATION_CACHE)
                .resolve(UUID.randomUUID().toString() + ".tar.gz");
        logger.info("Compressing {} files.", files.size());
        try {
            long unpackedSize = 0;
            OutputStream outputStream = java.nio.file.Files.newOutputStream(destination);
            OutputStream gZipOutputStream = new GzipCompressorOutputStream(outputStream);
            TarArchiveOutputStream tarArchiveOutputStream = new TarArchiveOutputStream(gZipOutputStream);

            // http://commons.apache.org/proper/commons-compress/examples.html
            for (File file : files) {
                TarArchiveEntry tarArchiveEntry = new TarArchiveEntry(file);
                tarArchiveEntry.setSize(file.length());
                unpackedSize += file.length();
                tarArchiveOutputStream.putArchiveEntry(tarArchiveEntry);
                IOUtils.copy(new FileInputStream(file), tarArchiveOutputStream);
                tarArchiveOutputStream.closeArchiveEntry();
            }

            gZipOutputStream.close();
            outputStream.close();

            return CompressedArchiveDetails.builder()
                    .location(destination)
                    .filecount(files.size())
                    .integrity("TODO")
                    .shasum("TODO")
                    .unpackedSize(unpackedSize)
                    .build();
        } catch (IOException e) {
            String message = "Failed to create package.";
            logger.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    private static Collection<File> getPublishableFiles(Path path) {
        Collection<File> files = FileUtils.listFiles(
                path.toFile(), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);

        return files.stream()
                .filter(PublishableProject::isPublishableFile)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static boolean isProtoFile(File file) {
        String extension = getExtension(file);
        return Objects.equals(extension, PROTO_FILE_EXT);
    }


    private static boolean isPublishableFile(File file) {
        String extension = getExtension(file);
        return publishableFileExtensions.contains(extension)
                || publishableFileNames.contains(file.getName());
    }

    @SuppressWarnings("UnstableApiUsage")
    private static String getExtension(File file) {
        return Files.getFileExtension(file.getName());
    }

    @Getter
    @Builder
    public static class CompressedArchiveDetails {

        private final Path location;

        private final int filecount;

        private final String integrity;

        private final String shasum;

        private final String signature;

        private final long unpackedSize;
    }
}
