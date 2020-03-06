package io.protop.core.cache;

import io.protop.core.manifest.ProjectCoordinate;
import io.protop.version.Version;
import io.reactivex.Completable;

public class CacheService {

    public Completable install(ProjectCoordinate coordinate, Version version) {
        // TODO
        return Completable.complete();
    }
}
