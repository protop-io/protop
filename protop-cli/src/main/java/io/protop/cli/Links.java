package io.protop.cli;

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
            description = "Clean everything from the cache.")
    public static class Clean implements Runnable {

        private static final Logger logger = Logger.getLogger(Cache.Clean.class);

        @CommandLine.ParentCommand
        private Links links;

        public void run() {
            Logs.enableIf(links.protop.isDebugMode());

            try {
                new LinkService().clean();
                handleSuccess();
            } catch (Exception e) {
                handleError(e);
            }
        }

        private void handleSuccess() {
            logger.always("Success!");
        }

        private void handleError(Throwable t) {
            logger.error("Failed to remove all links.", t);
            if (!Logs.areEnabled()) {
                logger.always("Something went wrong. Try again with -d to enable debug logs.");
            }
        }
    }
}
