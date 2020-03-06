package io.protop.core.sync;

import io.protop.core.manifest.Manifest;
import io.protop.core.manifest.DependencyMap;
import io.protop.core.manifest.ProjectCoordinate;
import io.protop.core.error.ServiceError;
import io.protop.core.error.ServiceException;
import io.protop.core.storage.Storage;
import io.protop.core.storage.StorageService;
import io.protop.core.sync.status.SyncStatus;
import io.protop.core.sync.status.Syncing;
import io.protop.version.Version;
import io.reactivex.Observable;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SyncService {

    private final StorageService storageService;

    public Observable<SyncStatus> sync(DependencyResolutionContext context) {
        Path projectPath = context.projectPath;

        Manifest manifest = Manifest.from(projectPath)
                .orElseThrow(() -> new ServiceException(ServiceError.MANIFEST_ERROR,
                        "Project manifest is missing."));

        return sync(manifest, context);
    }

    /**
     * Sync dependencies.
     * @param manifest project manifest.
     * @param context context details.
     * @return unresolved dependencies.
     */
    private Observable<SyncStatus> sync(Manifest manifest, DependencyResolutionContext context) {
        return Observable.create(emitter -> {
            Path protopPath = context.projectPath.resolve(Storage.ProjectDirectory.PROTOP.getName());
            storageService.createDirectoryIfNotExists(protopPath)
                    .blockingAwait();

            Path dependenciesPath = protopPath.resolve(Storage.ProjectDirectory.DEPS.getName());
            storageService.createDirectoryIfNotExists(dependenciesPath)
                    .blockingAwait();

            List<DependencyResolver> resolvers = new ArrayList<>();
            if (context.includesLinkedDependencies) {
                resolvers.add(new LinkedDependencyResolver());
            }

            // Currently always authorize cached dependencies and published dependencies.
            resolvers.add(new ExternalDependencyResolver());

            DependencyMap dependencyMap = Optional.ofNullable(manifest.getDependencies())
                    .orElseGet(DependencyMap::new);

            AtomicReference<Map<ProjectCoordinate, Version>> unresolvedDependencies = new AtomicReference<>(
                    dependencyMap.getValues());

            resolvers.forEach(resolver -> {
                emitter.onNext(new Syncing(resolver.getShortDescription()));
                Map<ProjectCoordinate, Version> next = resolver.resolve(dependenciesPath, unresolvedDependencies.get())
                        .blockingGet();
                unresolvedDependencies.set(next);
            });

            Map<ProjectCoordinate, Version> ultimatelyUnresolved = unresolvedDependencies.get();
            if (!ultimatelyUnresolved.isEmpty()) {
                emitter.onError(new IncompleteSync(ultimatelyUnresolved));
            } else {
                emitter.onComplete();
            }
        });
    }
}
