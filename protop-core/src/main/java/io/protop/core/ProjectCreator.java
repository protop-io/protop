package io.protop.core;

import io.protop.core.manifest.Manifest;

import java.nio.file.Path;

public interface ProjectCreator {

    void create(Manifest manifest, Path location);
}
