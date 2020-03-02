package io.protop.core.sync;

import io.protop.calver.CalVer;
import io.protop.core.config.ProjectId;
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
    Single<Map<ProjectId, CalVer>> resolve(
            Path dependencyDir, Map<ProjectId, CalVer> unresolvedDependencies);
}
