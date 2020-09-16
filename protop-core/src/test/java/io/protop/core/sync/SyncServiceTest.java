package io.protop.core.sync;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(SoftAssertionsExtension.class)
class SyncServiceTest {
    Path workspace;

    @BeforeEach
    public void setUp(@TempDir Path workspace) {
        this.workspace = workspace;
    }

    @Test
    @DisplayName("Should throw exception for non-directory.")
    public void shouldThrowExceptionForNonDirectory() throws Exception {
        final var syncService = new SyncService(null, null, null);

        final var file = workspace.resolve("file");

        Files.createFile(file);

        assertThatThrownBy(() -> syncService.filterValidChildren(null, file.toFile(), null))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should handle empty directory.")
    public void shouldHandleEmptyDirectory(final SoftAssertions softly) throws Exception {
        final var syncService = new SyncService(null, null, null);

        final var directoryWithFiles = new SyncService.DirectoryWithFiles();

        syncService.filterValidChildren(directoryWithFiles, workspace.toFile(), Path.of(""));

        softly.assertThat(directoryWithFiles.getFiles())
                .isEmpty();
        softly.assertThat(directoryWithFiles.getSubdirectories())
                .isEmpty();
    }

    @Test
    @DisplayName("Should handle non-proto file.")
    public void shouldHandleNonProtoFile(final SoftAssertions softly) throws Exception {
        final var syncService = new SyncService(null, null, null);

        final var file = workspace.resolve("file");

        Files.createFile(file);

        final var directoryWithFiles = new SyncService.DirectoryWithFiles();

        syncService.filterValidChildren(directoryWithFiles, workspace.toFile(), Path.of(""));

        softly.assertThat(directoryWithFiles.getFiles())
                .isEmpty();
        softly.assertThat(directoryWithFiles.getSubdirectories())
                .isEmpty();
    }

    @Test
    @DisplayName("Should handle proto file.")
    public void shouldHandleProtoFile(final SoftAssertions softly) throws Exception {
        final var syncService = new SyncService(null, null, null);

        final var file = workspace.resolve("file.proto");

        Files.createFile(file);

        final var directoryWithFiles = new SyncService.DirectoryWithFiles();

        syncService.filterValidChildren(directoryWithFiles, workspace.toFile(), Path.of(""));

        // noinspection unchecked
        softly.assertThat(directoryWithFiles.getFiles())
                .containsExactly(new AbstractMap.SimpleEntry<>("file.proto", new FileWithRootDir(null, file)));
        softly.assertThat(directoryWithFiles.getSubdirectories())
                .isEmpty();
    }

    @Test
    @DisplayName("Should throw exception for already defined proto file.")
    public void shouldThrowExceptionForAlreadyDefinedProtoFile() throws Exception {
        final var syncService = new SyncService(null, null, null);

        final var file = workspace.resolve("file.proto");

        Files.createFile(file);

        final var directoryWithFiles = new SyncService.DirectoryWithFiles();
        directoryWithFiles.getFiles().put("file.proto", new FileWithRootDir(null, file));

        assertThatThrownBy(() -> syncService.filterValidChildren(directoryWithFiles, workspace.toFile(), Path.of("")))
                .isInstanceOf(FileAlreadyExistsException.class);
    }

    @Test
    @DisplayName("Should handle proto files in subdirectories.")
    public void shouldHandleProtoFilesInSubdirectories(final SoftAssertions softly) throws Exception {
        final var syncService = new SyncService(null, null, null);

        final var directory = workspace.resolve("directory");
        final var file = directory.resolve("file.proto");

        Files.createDirectories(file.getParent());
        Files.createFile(file);

        final var directoryWithFiles = new SyncService.DirectoryWithFiles();

        syncService.filterValidChildren(directoryWithFiles, workspace.toFile(), Path.of(""));

        softly.assertThat(directoryWithFiles.getFiles())
                .isEmpty();
        // noinspection unchecked
        softly.assertThat(directoryWithFiles.getSubdirectories())
                .containsExactly(
                        new AbstractMap.SimpleEntry<>(
                                "directory",
                                new SyncService.DirectoryWithFiles(
                                        Collections.singletonMap("file.proto", new FileWithRootDir(null, file)),
                                        new HashMap<>())));
    }
}