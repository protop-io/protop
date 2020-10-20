package io.protop.core.sync;

import io.protop.core.Context;
import io.protop.core.manifest.Manifest;
import io.protop.core.storage.StorageService;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, SoftAssertionsExtension.class})
class SyncServiceTest {
    final File TEST_DATA_DIR = new File(System.getenv("TEST_DATA_DIR"));

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
                                        Collections.emptyMap())));
    }

    @Test
    @DisplayName("Should link all proto files.")
    public void shouldLinkAllProtoFiles(
            final SoftAssertions softly,
            @Mock final Context contextMock) throws Exception {
        when(contextMock.getProjectLocation())
                .thenReturn(workspace);

        final var syncService = new SyncService(null, new StorageService(), contextMock);

        FileUtils.copyDirectory(TEST_DATA_DIR, workspace.toFile());

        final var protopDir = workspace.resolve(".protop");
        final var depsDir = protopDir.resolve("deps");

        syncService.mergeDepsToPath(depsDir);

        final var pathDir = protopDir.resolve("path");

        softly.assertThat(pathDir.resolve("package000/file000.proto"))
                .isSymbolicLink();
        softly.assertThat(pathDir.resolve("package000/package0000/file0000.proto"))
                .isSymbolicLink();
        softly.assertThat(pathDir.resolve("package001/file001.proto"))
                .isSymbolicLink();
        softly.assertThat(pathDir.resolve("src/main/proto/package100/file100.proto"))
                .isSymbolicLink();
    }
}
