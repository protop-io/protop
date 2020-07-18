package io.protop.cli;

import io.protop.cli.errors.ExceptionHandler;
import io.protop.core.Context;
import io.protop.core.RuntimeConfiguration;
import io.protop.core.auth.AuthService;
import io.protop.core.auth.BasicAuthService;
import io.protop.core.grpc.GrpcService;
import io.protop.core.logs.Logger;
import io.protop.core.logs.Logs;
import io.protop.core.storage.StorageService;
import io.protop.core.sync.DependencyResolutionConfiguration;
import io.protop.core.sync.SyncService;
import io.protop.core.sync.status.SyncStatus;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.nio.file.Path;
import java.util.Optional;

@Command(name = "sync",
        description = "Sync project dependencies.")
public class Sync implements Runnable {

    private static final Logger logger = Logger.getLogger(Link.class);

    @ParentCommand
    private ProtopCli protop;

    @Option(names = {"-l", "--use-links"},
            description = "Allow dependencies to resolve to locally linked projects.",
            required = false,
            arity = "0..1",
            defaultValue = "false")
    private Boolean includeLinkedDependencies;

    @Option(names = {"-r", "--registry"},
            description = "Registry URI",
            required = false,
            arity = "0..1",
            defaultValue = "")
    private String registry;

    @Option(names = {"-g", "--git-refresh"},
            description = "Refresh Git projects sources.",
            required = false,
            arity = "0..1",
            defaultValue = "")
    private Boolean refreshGitSources;

    @Override
    public void run() {
        Logs.enableIf(protop.isDebugMode());
        new ExceptionHandler().run(() -> {
            Path location = Path.of(".").toAbsolutePath();

            RuntimeConfiguration cliRc = RuntimeConfiguration.builder()
                    .repositoryUrl(Optional.ofNullable(registry).orElse(null))
                    .refreshGitSources(refreshGitSources)
                    .build();
            Context context = Context.from(location, cliRc);

            StorageService storageService = new StorageService();
            AuthService<?> authService = new BasicAuthService(storageService);
            DependencyResolutionConfiguration resolutionContext = DependencyResolutionConfiguration.builder()
                    .includesLinkedDependencies(includeLinkedDependencies)
                    .build();
            GrpcService grpcService = new GrpcService(authService);
            SyncService syncService = new SyncService(authService, storageService, context, grpcService);

            syncService.sync(resolutionContext)
                    .doOnComplete(this::handleSuccess)
                    .doOnNext(this::handleNextStatus)
                    .blockingSubscribe();
        });
    }

    private void handleNextStatus(SyncStatus status) {
        logger.always(status.getMessage());
    }

    private void handleSuccess() {
        logger.always("Done syncing.");
    }
}
