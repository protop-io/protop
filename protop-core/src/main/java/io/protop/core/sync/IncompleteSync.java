package io.protop.core.sync;

import io.protop.core.manifest.ProjectCoordinate;
import io.protop.core.version.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class IncompleteSync extends Exception {

    private final Map<ProjectCoordinate, Version> unresolvedDependencies;
}
