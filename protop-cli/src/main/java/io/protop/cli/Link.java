package io.protop.cli;

import io.protop.cli.errors.ExceptionHandler;
import io.protop.core.link.LinkService;
import io.protop.core.logs.Logger;
import io.protop.core.logs.Logs;
import io.protop.core.publish.PublishableProject;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.nio.file.Path;

@Command(name = "link",
        description = "Link a local project.")
public class Link implements Runnable {

    private static final Logger logger = Logger.getLogger(Link.class);

    @ParentCommand
    private ProtopCli protop;

    @CommandLine.Parameters(arity = "0..1",
            description = "Root directory of project",
            defaultValue = ".")
    private Path directory;

    @Override
    public void run() {
        Logs.enableIf(protop.isDebugMode());
        new ExceptionHandler().run(() -> {
            new LinkService().link(PublishableProject.from(directory.toAbsolutePath()));
        });
    }
}
