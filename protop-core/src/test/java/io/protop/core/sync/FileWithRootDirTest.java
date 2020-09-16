package io.protop.core.sync;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FileWithRootDirTest {
    Path protopPath;
    Path protopDeps;

    @BeforeEach
    public void setUp(@TempDir final Path workspace) {
        protopPath = workspace.resolve(".protop/path");
        protopDeps = workspace.resolve(".protop/deps");
    }

    @Test
    @DisplayName("Should create symlink using null rootDir.")
    public void shouldCreateSymlinkUsingNullRootDir(@TempDir final Path workspace) throws Exception {
        final var fileWithRootDir = new FileWithRootDir(null, protopDeps.resolve("org/name/package/file.proto"));

        fileWithRootDir.createSymbolicLink(protopPath, protopPath.resolve("package/file.proto"));

        assertThat(protopPath.resolve("package/file.proto"))
                .isSymbolicLink();
    }

    @Test
    @DisplayName("Should create symlink using rootDir.")
    public void shouldCreateSymlinkUsingRootDir(@TempDir final Path workspace) throws Exception {
        final var rootDir = Path.of("src/main/proto");
        final var fileWithRootDir = new FileWithRootDir(rootDir, protopDeps.resolve("org/name/src/main/proto/package/file.proto"));

        fileWithRootDir.createSymbolicLink(protopPath, protopPath.resolve("src/main/proto/package/file.proto"));

        assertThat(protopPath.resolve("package/file.proto"))
                .isSymbolicLink();
    }
}