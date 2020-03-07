package io.protop.core.sync;

import io.protop.core.Context;
import io.protop.core.auth.AuthService;
import io.protop.core.cache.CacheService;
import io.protop.core.logs.Logger;
import io.protop.core.manifest.DependencyMap;
import io.protop.core.manifest.ProjectCoordinate;
import io.protop.core.storage.Storage;
import io.protop.core.storage.StorageService;
import io.protop.core.sync.status.SyncStatus;
import io.protop.core.sync.status.Syncing;
import io.protop.version.Version;
import io.reactivex.Observable;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class SyncService {

    private static final Logger logger = Logger.getLogger(SyncService.class);

    private final AuthService<?> authService;
    private final StorageService storageService;
    private final Context context;
    private final CacheService cacheService;

    public SyncService(AuthService<?> authService,
                       StorageService storageService,
                       Context context) {
        this.authService = authService;
        this.storageService = storageService;
        this.context = context;
        this.cacheService = new CacheService(storageService);
    }

    /**
     * Sync dependencies.
     * @param dependencyResolutionConfiguration dependency resolution details.
     * @return unresolved dependencies.
     */
    public Observable<SyncStatus> sync(DependencyResolutionConfiguration dependencyResolutionConfiguration) {
        return Observable.create(emitter -> {
            Path protopPath = context.getProjectLocation()
                    .resolve(Storage.ProjectDirectory.PROTOP.getName());
            storageService.createDirectoryIfNotExists(protopPath)
                    .blockingAwait();

            Path dependenciesDir = protopPath.resolve(Storage.ProjectDirectory.DEPS.getName());

            storageService.createDirectoryIfNotExists(dependenciesDir)
                    .blockingAwait();

            List<DependencyResolver> resolvers = new ArrayList<>();
            if (dependencyResolutionConfiguration.includesLinkedDependencies) {
                resolvers.add(new LinkedDependencyResolver());
            }

            // Currently always authorize cached dependencies and published dependencies.
            resolvers.add(new ExternalDependencyResolver(authService, cacheService, context));

            DependencyMap dependencyMap = Optional.ofNullable(context.getManifest().getDependencies())
                    .orElseGet(DependencyMap::new);

            AtomicReference<Map<ProjectCoordinate, Version>> unresolvedDependencies = new AtomicReference<>(
                    dependencyMap.getValues());

            resolvers.forEach(resolver -> {
                emitter.onNext(new Syncing(resolver.getShortDescription()));
                Map<ProjectCoordinate, Version> next = resolver.resolve(dependenciesDir, unresolvedDependencies.get())
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
