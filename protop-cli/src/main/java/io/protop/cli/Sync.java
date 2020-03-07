package io.protop.cli;

import io.protop.core.Context;
import io.protop.core.RuntimeConfiguration;
import io.protop.core.auth.AuthService;
import io.protop.core.auth.BasicAuthService;
import io.protop.core.logs.Logger;
import io.protop.core.logs.Logs;
import io.protop.core.storage.StorageService;
import io.protop.core.sync.DependencyResolutionConfiguration;
import io.protop.core.sync.IncompleteSync;
import io.protop.core.sync.SyncService;
import io.protop.core.sync.status.SyncStatus;
import java.nio.file.Path;
import java.util.Optional;

import io.protop.utils.UriUtils;
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

    @Option(names = {"-r", "--registry"},
            description = "Registry",
            required = false,
            arity = "0..1",
            defaultValue = "")
    private String registry;

    @Override
    public void run() {
        Logs.enableIf(protop.isDebugMode());

        Path location = Path.of(".");

        RuntimeConfiguration cliRc = RuntimeConfiguration.builder()
                .repositoryUri(Optional.ofNullable(registry)
                        .map(UriUtils::fromString)
                        .orElse(null))
                .build();
        Context context = Context.from(location, cliRc);

        StorageService storageService = new StorageService();
        AuthService<?> authService = new BasicAuthService(storageService);
        DependencyResolutionConfiguration resolutionContext = DependencyResolutionConfiguration.builder()
                .includesLinkedDependencies(includeLinkedDependencies)
                .build();
        SyncService syncService = new SyncService(authService, storageService, context);

        syncService.sync(resolutionContext)
                .subscribe(this::handleNextStatus, this::handleError, this::handleSuccess)
                .dispose();
    }

    private void handleNextStatus(SyncStatus status) {
        logger.always(status.getMessage());
    }

    private void handleError(Throwable error) {
        logger.always("Failed to createSymbolicLink some dependencies.");

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
