package io.protop.core.sync;

import io.protop.core.link.LinkedProjectsMap;
import io.protop.core.logs.Logger;
import io.protop.core.manifest.PackageId;
import io.protop.core.manifest.revision.RevisionSource;
import io.reactivex.Single;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LinkedDependencyResolver implements DependencyResolver {

    private static final Logger logger = Logger.getLogger(LinkedDependencyResolver.class);

    @Override
    public String getShortDescription() {
        return "linked projects";
    }

    @Override
    public Single<Map<PackageId, RevisionSource>> resolve(Path dependencyDir,
                                                          Map<PackageId, RevisionSource> unresolvedDependencies) {
        return Single.fromCallable(() -> {
            LinkedProjectsMap resolvable = LinkedProjectsMap.load()
                    .blockingGet();
            Set<PackageId> resolved = new HashSet<>();

            unresolvedDependencies.forEach((name, revision) -> {
                if (resolvable.getProjects().containsKey(name)) {
                    try {
                        SyncUtils.createSymbolicLink(dependencyDir, name, resolvable.getProjects().get(name));
                        resolved.add(name);
                    } catch (IOException e) {
                        logger.error("Could not create link to dependency.", e);
                        throw new RuntimeException(e);
                    }
                }
            });

            resolved.forEach(unresolvedDependencies::remove);
            return unresolvedDependencies;
        });
    }
}
