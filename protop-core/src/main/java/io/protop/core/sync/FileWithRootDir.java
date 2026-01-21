package io.protop.core.sync;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@EqualsAndHashCode
@Getter
class FileWithRootDir {
    private final Path rootDir;
    private final Path target;

    public FileWithRootDir(final Path rootDir, final File file) {
        this(rootDir, file.toPath());
    }

    public FileWithRootDir(final Path rootDir, final Path target) {
        this.rootDir = rootDir != null ? rootDir : Path.of("");
        this.target = target;
    }

    public void createSymbolicLink(final Path protopPathDir, final Path linkWithRootDir) throws IOException {
        final Path linkRelative = protopPathDir
                .resolve(rootDir)
                .relativize(linkWithRootDir);
        final Path linkWithoutRootDir = protopPathDir.resolve(linkRelative);

        Files.createDirectories(linkWithoutRootDir.getParent());
        Files.deleteIfExists(linkWithoutRootDir);
        Files.createSymbolicLink(linkWithoutRootDir, target);
    }
}
