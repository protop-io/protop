package io.protop.cli.errors;

import io.protop.core.auth.AuthenticationFailed;
import io.protop.core.error.PackageNotFound;
import io.protop.core.error.ProjectAlreadyCreated;
import io.protop.core.error.ServiceException;
import io.protop.core.error.ServiceExceptionConsumer;
import io.protop.core.logs.Logger;
import io.protop.core.logs.Logs;
import io.protop.core.manifest.InvalidDependencyName;
import io.protop.core.manifest.ManifestNotFound;
import io.protop.core.manifest.revision.InvalidRevision;
import io.protop.core.publish.PublishFailed;
import io.protop.core.storage.StorageException;
import io.protop.core.sync.IncompleteSync;
import io.protop.core.manifest.revision.InvalidVersionString;

/**
 * Handles service exceptions, which are fail-fast exceptions and often can be
 * thrown from different places.
 */
public final class ExceptionHandler implements ServiceExceptionConsumer {

    private static final Logger logger = Logger.getLogger(ExceptionHandler.class);

    private static final String RETRY_WITH_DEBUG = "Retry with -d to enable debug logs";

    @FunctionalInterface
    public interface ExceptionThrowingRunnable {

        void run() throws Throwable;
    }

    public void run(ExceptionThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Throwable thrown) {
            handle(thrown);
        }
    }

    private void handle(Throwable thrown) {
        if (thrown instanceof ServiceException) {
            handle((ServiceException) thrown);
        } else if (thrown instanceof InvalidVersionString) {
            handle((InvalidVersionString) thrown);
        } else {
            // Fallback
            if (!Logs.areEnabled()) {
                logger.always("An unexpected error occurred.");
                logger.always(RETRY_WITH_DEBUG);
            } else {
                logger.error("An unexpected error occurred.", thrown);
            }
        }
    }

    private void handle(ServiceException serviceException) {
        serviceException.accept(this);
    }


    private void handle(InvalidVersionString invalidVersionString) {
        logger.always(invalidVersionString.getMessage());
    }

    @Override
    public void consume(PackageNotFound packageNotFound) {
        logger.always(packageNotFound.getMessage());
    }

    @Override
    public void consume(IncompleteSync incompleteSync) {
        if (incompleteSync.getUnresolvedDependencies().isEmpty()) {
            logger.always("Failed to resolve dependencies.");
            logger.always(RETRY_WITH_DEBUG);
        } else {
            logger.always("Failed to resolve dependencies:");
            incompleteSync.getUnresolvedDependencies().forEach((dependencyName, version) ->
                    logger.always(String.format("  - %s %s", dependencyName, version.toString())));
        }
    }

    @Override
    public void consume(ProjectAlreadyCreated projectAlreadyCreated) {
        logger.always(projectAlreadyCreated.getMessage());
    }

    @Override
    public void consume(StorageException storageException) {
        logger.always(storageException.getMessage());
        if (!Logs.areEnabled()) {
            logger.always(RETRY_WITH_DEBUG);
        }
    }

    @Override
    public void consume(InvalidDependencyName invalidDependencyName) {
        logger.always(invalidDependencyName.getMessage());
        if (!Logs.areEnabled()) {
            logger.always(RETRY_WITH_DEBUG);
        }
    }

    @Override
    public void consume(PublishFailed publishFailed) {
        logger.always(publishFailed.getMessage());
        if (!Logs.areEnabled()) {
            logger.always(RETRY_WITH_DEBUG);
        }
    }

    @Override
    public void consume(AuthenticationFailed authenticationFailed) {
        logger.always(authenticationFailed.getMessage());
    }

    @Override
    public void consume(ManifestNotFound manifestNotFound) {
        logger.always(manifestNotFound.getMessage());
    }

    @Override
    public void consume(InvalidRevision invalidRevision) {
        logger.always(invalidRevision.getMessage());
    }
}
