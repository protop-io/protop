package io.protop.cli.errors;

import io.protop.core.logs.Logger;
import io.protop.core.sync.IncompleteSync;

public final class ExceptionHandler {

    private static final Logger logger = Logger.getLogger(ExceptionHandler.class);

    @FunctionalInterface
    public interface ExceptionThrowingRunnable {

        void run() throws Throwable;
    }

    public void run(ExceptionThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            handle(t);
        }
    }

    private void handle(Throwable thrown) {
        if (thrown instanceof IncompleteSync) {
            logger.always("Failed to resolve some dependencies.");
            ((IncompleteSync) thrown).getUnresolvedDependencies().forEach((dependencyName, version) ->
                    logger.always(String.format("  - (unresolved) %s %s", dependencyName, version.toString())));
        } else {
            // Fallback
            // TODO handle better
            logger.error("An unexpected error occurred.", thrown);
        }
    }
}
