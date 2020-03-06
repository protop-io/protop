package io.protop.core.sync;

import io.protop.core.cache.CachedProjectsMap;
import io.protop.core.error.ServiceException;
import io.protop.core.manifest.ProjectCoordinate;
import io.protop.version.Version;
import io.reactivex.Single;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;

/**
 * Resolves dependencies published to a registry.
 */
@AllArgsConstructor
public class ExternalDependencyResolver implements DependencyResolver {

    @Override
    public String getShortDescription() {
        return "registered dependencies";
    }

    @Override
    public Single<Map<ProjectCoordinate, Version>> resolve(
            Path dependencyDir, Map<ProjectCoordinate, Version> unresolvedDependencies) {
        return Single.fromCallable(() -> {
            CachedProjectsMap cache = CachedProjectsMap.load()
                    .blockingGet();

            Set<ProjectCoordinate> resolved = new HashSet<>();

            unresolvedDependencies.forEach((coordinate, version) -> {
                Map<ProjectCoordinate, Map<Version, Path>> projects = cache.getProjects();
                if (projects.containsKey(coordinate)) {
                    Map<Version, Path> versions = projects.get(coordinate);
                    try {
                        if (!versions.containsKey(version)) {
                            retrieveAndCache(coordinate, version, versions);
                        }
                        resolve(dependencyDir, coordinate, version, versions.get(version));
                        resolved.add(coordinate);
                    } catch (IOException e) {
                        throw new ServiceException("Unexpectedly failed to resolve external dependencies.", e);
                    }
                }
            });

            resolved.forEach(unresolvedDependencies::remove);

            return unresolvedDependencies;
        });
    }

    private void retrieveAndCache(ProjectCoordinate coordinate, Version version, Map<Version, Path> versions) {
        // TODO

        // TODO
        versions.put(version, null);
    }

    private void resolve(Path dependencyDir, ProjectCoordinate coordinate, Version version, Path src) throws IOException {
        Path orgPath = dependencyDir.resolve(coordinate.getOrganizationId());

        if (!Files.isDirectory(orgPath)) {
            Files.deleteIfExists(orgPath);
            Files.createDirectory(orgPath);
        }

        Path projectPath = orgPath.resolve(coordinate.getProjectId());

        if (!Files.isDirectory(projectPath)) {
            Files.deleteIfExists(projectPath);
            Files.createDirectory(projectPath);
        }

        Path versionPath = projectPath.resolve(version.toString());

        if (Files.exists(versionPath)) {
            if (Files.isSymbolicLink(versionPath)) {
                return;
            } else {
                Files.delete(versionPath);
            }
        }

        Files.createSymbolicLink(versionPath, src);
    }
}
