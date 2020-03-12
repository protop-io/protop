package io.protop.core.sync;

import io.protop.core.manifest.ProjectCoordinate;
import io.protop.core.version.Version;
import io.reactivex.Single;

import java.nio.file.Path;
import java.util.Map;

public interface DependencyResolver {

    /**
     * @return short description of what dependencies the resolver resolves (for logging purposes).
     */
    String getShortDescription();

    /**
     * Resolve dependencies.
     * @return unresolved dependencies.
     */
    Single<Map<ProjectCoordinate, Version>> resolve(
            Path dependencyDir, Map<ProjectCoordinate, Version> unresolvedDependencies);
}
