package io.protop.core.sync;

import io.protop.core.manifest.PackageId;
import io.protop.core.manifest.revision.RevisionSource;
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
    Single<Map<PackageId, RevisionSource>> resolve(
            Path dependencyDir, Map<PackageId, RevisionSource> unresolvedDependencies);
}
