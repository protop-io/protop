package io.protop.core.publishing;

import java.net.URI;

public interface ProjectPublisher {

    /**
     * Publish project to a registry.
     * @param project project to link.
     * @param registry registry to link the project.
     */
    void publish(PublishableProject project, URI registry);
}
