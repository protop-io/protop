package io.protop.cli;

import io.protop.core.Environment;
import io.protop.core.logs.Logger;
import io.protop.core.logs.Logs;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;

@Command(name = "protop",
        header = {
                "@|cyan              _            |@",
                "@|cyan  ___ ___ ___| |_ ___ ___  |@",
                "@|cyan | . |  _| . |  _| . | . | |@",
                "@|cyan |  _|_| |___|_| |___|  _| |@",
                "@|cyan |_|                 |_|   |@",
                ""
        },
        subcommands = {
                HelpCommand.class,
                Init.class,
                // These are orphaned features for now; to use during development,
                //  simply uncomment them.
//                Publish.class,
//                Login.class,
//                Logout.class,
                Link.class,
                Unlink.class,
                Links.class,
                Sync.class,
                Cache.class
        },
        description = "...")
class ProtopCli implements Runnable {

    private static final Logger logger = Logger.getLogger(ProtopCli.class);

    @Getter
    @Option(names = {"-d", "--debug"},
            required = false,
            defaultValue = "false",
            description = "Turn on debug logs.")
    private boolean debugMode;

    @Option(names = {"-v", "--revision"},
            required = false,
            defaultValue = "false",
            description = "Print the current revision of protop.")
    private Boolean logVersion;

    public void run() {
        Logs.enableIf(debugMode);

        if (logVersion) {
            logVersion();
        } else {
            new CommandLine(new ProtopCli()).execute("help");
        }
    }

    private void logVersion() {
        Environment.getInstance().getVersion().ifPresentOrElse(
                v -> logger.always("protop " + v),
                () -> logger.always("Could not determine the current revision of protop."));
    }

    public static void main(String... args) {
        new CommandLine(new ProtopCli()).execute(args);
    }
}
