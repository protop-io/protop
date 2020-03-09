package io.protop.cli;

import io.protop.core.link.LinkService;
import io.protop.core.logs.Logger;
import io.protop.core.logs.Logs;
import io.protop.core.publish.PublishableProject;
import picocli.CommandLine;

import java.nio.file.Path;

@CommandLine.Command(name = "unlink",
        description = "Unlink a local project.")
public class Unlink implements Runnable {

    private static final Logger logger = Logger.getLogger(Link.class);

    @CommandLine.ParentCommand
    private ProtopCli protop;

    @Override
    public void run() {
        Logs.enableIf(protop.isDebugMode());

        Path location = Path.of(".");

        new LinkService().unlink(PublishableProject.from(location));
    }
}
