package io.protop.cli;

import io.protop.cli.errors.ExceptionHandler;
import io.protop.core.link.LinkService;
import io.protop.core.logs.Logger;
import io.protop.core.logs.Logs;
import picocli.CommandLine;

import java.util.concurrent.atomic.AtomicReference;

@CommandLine.Command(name = "links",
        subcommands = {
                Links.Clean.class,
                Links.List.class
        },
        description = "Manipulate the system-wide linked projects.")
public class Links implements Runnable {

    private static final Logger logger = Logger.getLogger(Link.class);

    @CommandLine.ParentCommand
    private ProtopCli protop;

    @Override
    public void run() {
        new CommandLine(new ProtopCli()).execute("help", "links");
    }

    @CommandLine.Command(name = "clean",
            description = "Unlink all currently linked projects.")
    public static class Clean implements Runnable {

        private static final Logger logger = Logger.getLogger(Cache.Clean.class);

        @CommandLine.ParentCommand
        private Links links;

        public void run() {
            Logs.enableIf(links.protop.isDebugMode());
            new ExceptionHandler().run(() -> {
                new LinkService().clean().blockingAwait();
                handleSuccess();
            });
        }

        private void handleSuccess() {
            logger.always("Success!");
        }
    }

    @CommandLine.Command(name = "list",
            aliases = {"ls"},
            description = "List all currently linked projects.")
    public static class List implements Runnable {

        private static final Logger logger = Logger.getLogger(Cache.Clean.class);

        @CommandLine.ParentCommand
        private Links links;

        public void run() {
            Logs.enableIf(links.protop.isDebugMode());
            new ExceptionHandler().run(() -> {
                AtomicReference<Integer> total = new AtomicReference<>(0);
                new LinkService().list().subscribe(
                        detail -> {
                            Integer prevTotal = total.get();
                            if (prevTotal == 0) {
                                logger.always("Linked projects:");
                            }
                            total.set(prevTotal + 1);
                            logger.always(String.format(
                                    "  - %s (%s)",
                                    detail.getPackageId(),
                                    detail.getPath()));
                        },
                        err -> {
                            throw new RuntimeException("Failed to list linked repositories.", err);
                        },
                        () -> {
                            Integer finalTotal = total.get();
                            logger.always(String.format(
                                    "Total: %s project%s.",
                                    finalTotal,
                                    finalTotal == 1 ? "" : "s"));
                        }
                ).dispose();
            });
        }
    }
}
