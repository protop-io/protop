package io.protop.core.sync;

import io.protop.calver.CalVer;
import io.protop.core.config.Configuration;
import io.protop.core.config.DependencyMap;
import io.protop.core.config.ProjectId;
import io.protop.core.error.ServiceError;
import io.protop.core.error.ServiceException;
import io.protop.core.storage.StorageService;
import io.protop.core.sync.status.SyncStatus;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import io.protop.core.sync.status.Syncing;
import io.reactivex.Observable;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SyncService {

    private static final String PROTOP_DEPENDENCIES_DIR_NAME = "proto_include";

    private final StorageService storageService;

    public Observable<SyncStatus> sync(DependencyResolutionContext context) {
        Path projectPath = context.projectPath;

        Configuration configuration = Configuration.from(projectPath)
                .orElseThrow(() -> new ServiceException(ServiceError.CONFIGURATION_ERROR,
                        "Project configuration is missing."));

        return sync(configuration, context);
    }

    /**
     * Sync dependencies.
     * @param configuration project configuration.
     * @param context context details.
     * @return unresolved dependencies.
     */
    private Observable<SyncStatus> sync(
            Configuration configuration, DependencyResolutionContext context) {
        return Observable.create(emitter -> {
            Path dependenciesPath = context.projectPath.resolve(PROTOP_DEPENDENCIES_DIR_NAME);
            storageService.createDirectoryIfNotExists(dependenciesPath)
                    .blockingAwait();

            List<DependencyResolver> resolvers = new ArrayList<>();
            if (context.includesLinkedDependencies) {
                resolvers.add(new LinkedDependencyResolver());
            }

            // Currently always use cached dependencies and published dependencies.
            resolvers.add(new PublishedDependencyResolver());

            DependencyMap dependencyMap = Optional.ofNullable(configuration.getDependencies())
                    .orElseGet(DependencyMap::new);

            AtomicReference<Map<ProjectId, CalVer>> unresolvedDependencies = new AtomicReference<>(
                    dependencyMap.getValues());

            resolvers.forEach(resolver -> {
                emitter.onNext(new Syncing(resolver.getShortDescription()));
                Map<ProjectId, CalVer> next = resolver.resolve(dependenciesPath, unresolvedDependencies.get())
                        .blockingGet();
                unresolvedDependencies.set(next);
            });

            Map<ProjectId, CalVer> ultimatelyUnresolved = unresolvedDependencies.get();
            if (!ultimatelyUnresolved.isEmpty()) {
                emitter.onError(new IncompleteSync(ultimatelyUnresolved));
            } else {
                emitter.onComplete();
            }
        });
    }
}
