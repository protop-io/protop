package io.protop.core.publishing;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import io.protop.core.config.Configuration;
import io.protop.core.error.ServiceError;
import io.protop.core.error.ServiceException;
import javax.validation.constraints.NotNull;
import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
@AllArgsConstructor
public class PublishableProject {

    private static final Logger logger = LoggerFactory.getLogger(PublishableProject.class);
    private static final String TEMP_DISTRIBUTION_NAME = ".tmp.tar.gz";
    private static final String PROTO_FILE_EXT = "proto";

    @NotNull
    private final Configuration configuration;

    @NotNull
    private final Path projectLocation;

    @NotNull
    private final Collection<File> files;

    public static PublishableProject from(Path projectLocation) {
        Configuration configuration = Configuration.from(projectLocation)
                .orElseThrow(() -> new ServiceException(ServiceError.CONFIGURATION_ERROR, "Project configuration not found."));

        Collection<File> publishableFiles = getPublishableFiles(projectLocation);
        if (publishableFiles.isEmpty()) {
            throw new ServiceException("No files found to link in the selected path.");
        }

        return new PublishableProject(configuration, projectLocation, ImmutableList.copyOf(publishableFiles));
    }

    public Path compressAndZip() {
        return compressAndZip(projectLocation);
    }

    public Path compressAndZip(Path directory) {
        Path destination = directory.resolve(TEMP_DISTRIBUTION_NAME);
        try {
            OutputStream outputStream = java.nio.file.Files.newOutputStream(destination);
            OutputStream gZipOutputStream = new GzipCompressorOutputStream(outputStream);
            TarArchiveOutputStream tarArchiveOutputStream = new TarArchiveOutputStream(gZipOutputStream);

            // http://commons.apache.org/proper/commons-compress/examples.html
            for (File file : files) {
                TarArchiveEntry tarArchiveEntry = new TarArchiveEntry(file, file.getPath());
                tarArchiveEntry.setSize(file.length());
                tarArchiveOutputStream.putArchiveEntry(tarArchiveEntry);
                IOUtils.copy(new FileInputStream(file), tarArchiveOutputStream);
                tarArchiveOutputStream.closeArchiveEntry();
            }

            gZipOutputStream.close();
            outputStream.close();

            return destination;
        } catch (IOException e) {
            throw new ServiceException("Unable to create package.", e);
        }
    }

    private static Collection<File> getPublishableFiles(Path path) {
        Collection<File> files = FileUtils.listFiles(
                path.toFile(), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);

        return files.stream()
                .filter(PublishableProject::isProtoFile)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @SuppressWarnings("UnstableApiUsage")
    private static boolean isProtoFile(File file) {
        return Objects.equals(PROTO_FILE_EXT, Files.getFileExtension(file.getName()));
    }
}
