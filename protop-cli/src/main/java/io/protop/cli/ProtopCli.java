package io.protop.cli;

import io.protop.core.Environment;
import io.protop.core.logs.Logger;
import io.protop.core.logs.Logs;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.*;

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
                Publish.class,
                Login.class,
                Logout.class,
                Link.class,
                Sync.class
        },
        description = "...")
class ProtopCli implements Runnable {

    private static final Logger logger = Logger.getLogger(ProtopCli.class);

    @Getter
    @Option(names = {"-d", "--debug"},
            required = false,
            defaultValue = "false")
    private boolean debugMode;

    @Option(names = {"-v", "--version"},
            required = false,
            defaultValue = "false",
            description = "The version.")
    private Boolean logVersion;

    public void run() {
        Logs.enableIf(debugMode);

        if (logVersion) {
            logger.always("protop version " + Environment.getInstance().getVersion());
        } else {
            new CommandLine(new ProtopCli()).execute("help");
        }
    }

    public static void main(String... args) {
        new CommandLine(new ProtopCli()).execute(args);
    }
}
