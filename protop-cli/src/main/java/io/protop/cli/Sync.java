package io.protop.cli;

import io.protop.core.logs.Logger;
import io.protop.core.logs.Logs;
import io.protop.core.storage.StorageService;
import io.protop.core.sync.DependencyResolutionContext;
import io.protop.core.sync.IncompleteSync;
import io.protop.core.sync.SyncService;
import io.protop.core.sync.status.SyncStatus;
import java.nio.file.Path;
import picocli.CommandLine.*;

@Command(name = "sync",
        description = "Sync project and dependencies.")
public class Sync implements Runnable {

    private static final Logger logger = Logger.getLogger(Link.class);

    @ParentCommand
    private ProtopCli protop;

    @Option(names = {"-l", "--include_linked"},
            required = false,
            defaultValue = "true")
    private Boolean includeLinkedDependencies;

    @Override
    public void run() {
        Logs.enableIf(protop.isDebugMode());

        Path location = Path.of(".");
        StorageService storageService = new StorageService();
        DependencyResolutionContext context = DependencyResolutionContext.builder()
                .projectPath(location)
                .includesLinkedDependencies(includeLinkedDependencies)
                .build();
        SyncService syncService = new SyncService(storageService);

        syncService.sync(context)
                .subscribe(this::handleNextStatus, this::handleError, this::handleSuccess)
                .dispose();
    }

    private void handleNextStatus(SyncStatus status) {
        logger.always(status.getMessage());
    }

    private void handleError(Throwable error) {
        logger.always("Failed to resolve some dependencies.");

        if (error instanceof IncompleteSync) {
            ((IncompleteSync) error).getUnresolvedDependencies().forEach((dependencyName, version) ->
                        logger.always(String.format("  - (unresolved) %s %s", dependencyName, version.toString())));
        } else {
            logger.error("An unexpected error occurred.", error);
        }
    }

    private void handleSuccess() {
        logger.always("Done syncing.");
    }
}
