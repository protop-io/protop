package io.protop.cli;

import io.protop.cli.errors.ExceptionHandler;
import io.protop.core.link.LinkService;
import io.protop.core.logs.Logger;
import io.protop.core.logs.Logs;
import picocli.CommandLine;

@CommandLine.Command(name = "links",
        subcommands = {Links.Clean.class},
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
                new LinkService().clean();
                handleSuccess();
            });
        }

        private void handleSuccess() {
            logger.always("Success!");
        }
    }
}
