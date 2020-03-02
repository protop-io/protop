package io.protop.core;

import io.protop.core.config.Configuration;

import java.nio.file.Path;

public interface ProjectCreator {

    void create(Configuration configuration, Path location);
}
