package io.protop.core.sync;

import io.protop.calver.CalVer;
import io.protop.core.config.ProjectId;
import io.protop.core.error.ServiceException;
import io.protop.core.link.LinkedProjectsMap;
import io.protop.core.logs.Logger;
import io.reactivex.Single;
import java.io.IOException;
import java.nio.file.Files;
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
    public Single<Map<ProjectId, CalVer>> resolve(
            Path dependencyDir, Map<ProjectId, CalVer> unresolvedDependencies) {
        return Single.fromCallable(() -> {
            LinkedProjectsMap resolvable = LinkedProjectsMap.load()
                    .blockingGet();

            Set<ProjectId> resolved = new HashSet<>();

            unresolvedDependencies.forEach((name, version) -> {
                if (resolvable.getProjects().containsKey(name)) {
                    try {
                        resolve(dependencyDir, name, resolvable.getProjects().get(name));
                        resolved.add(name);
                    } catch (IOException e) {
                        throw new ServiceException("Unexpectedly failed to link dependencies.", e);
                    }
                }
            });

            resolved.forEach(unresolvedDependencies::remove);

            return unresolvedDependencies;
        });
    }

    private void resolve(Path dependencyDir, ProjectId name, Path src) throws IOException {
        Path org = dependencyDir.resolve(name.getOrganization());

        if (!Files.isDirectory(org)) {
            Files.deleteIfExists(org);
            Files.createDirectory(org);
        }

        Path project = org.resolve(name.getProject());
        if (Files.exists(project)) {
            if (Files.isSymbolicLink(project)) {
                return;
            } else {
                Files.delete(project);
            }
        }

        Files.createSymbolicLink(project, src);
    }
}
