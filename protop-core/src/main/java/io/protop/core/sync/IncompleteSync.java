package io.protop.core.sync;

import io.protop.core.manifest.ProjectCoordinate;
import java.util.Map;

import io.protop.version.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IncompleteSync extends Exception {

    private final Map<ProjectCoordinate, Version> unresolvedDependencies;
}
