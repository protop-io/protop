package io.protop.cli;

import io.protop.core.Context;
import io.protop.core.RuntimeConfiguration;
import io.protop.core.auth.BasicAuthService;
import io.protop.core.auth.AuthService;
import io.protop.core.error.ServiceException;
import io.protop.core.logs.Logger;
import io.protop.core.logs.Logs;
import io.protop.core.publish.ProjectPublisher;
import io.protop.core.publish.ProjectPublisherImpl;
import io.protop.core.publish.PublishableProject;
import io.protop.core.storage.StorageService;
import io.protop.utils.UriUtils;
import io.reactivex.Completable;

import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;

import picocli.CommandLine.*;

@Command(name = "publish",
        description = "")
public class Publish implements Runnable {

    private static final Logger logger = Logger.getLogger(Publish.class);

    @ParentCommand
    private ProtopCli protop;

    @Parameters(arity = "0..1",
            description = "Location of project",
            defaultValue = ".")
    private Path location;

    @Option(names = {"-r", "--registry"},
            description = "Registry",
            required = false,
            arity = "0..1",
            defaultValue = "")
    private String registry;

    @Override
    public void run() {

        Logs.enableIf(protop.isDebugMode());

        RuntimeConfiguration cliRc = RuntimeConfiguration.builder()
                .repositoryUri(Optional.ofNullable(registry)
                        .map(UriUtils::fromString)
                        .orElse(null))
                .build();
        Context context = Context.from(location, cliRc);
        StorageService storageService = new StorageService();
        AuthService authService = new BasicAuthService(storageService);
        ProjectPublisher projectPublisher = new ProjectPublisherImpl(context, authService);

        PublishableProject project = PublishableProject.from(location);

        handle(projectPublisher.publish(project));
    }

    private void handle(Completable completable) {
        completable.subscribe(() -> {
            // TODO better message
            logger.always("Published!");
        }, e -> {
            if (e instanceof ServiceException) {
                String message = String.format("Failed to publish: %s. Retry with -d for more details.", e.getMessage());
                logger.always(message);
            } else {
                if (!protop.isDebugMode()) {
                    logger.always("Something unexpected happened. Retry with -d for more details.");
                }
                logger.error(e.getMessage(), e);
            }
        }).dispose();
    }
}
