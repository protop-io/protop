package io.protop.core.publish;

import io.reactivex.Completable;

public interface ProjectPublisher {

    /**
     * Publish project to a registry.
     * @param project project to link.
     */
    Completable publish(PublishableProject project);
}
