package io.protop.core.sync;

import io.protop.calver.CalVer;
import io.protop.core.config.ProjectId;
import io.reactivex.Single;
import java.nio.file.Path;
import java.util.Map;

/**
 * Resolves dependencies published to the registry.
 */
public class PublishedDependencyResolver implements DependencyResolver {

    @Override
    public String getShortDescription() {
        return "registered dependencies";
    }

    @Override
    public Single<Map<ProjectId, CalVer>> resolve(
            Path dependencyDir, Map<ProjectId, CalVer> unresolvedDependencies) {
        return Single.fromCallable(() -> {
            // TODO load cache

            // TODO

            return unresolvedDependencies;
        });
    }
}
