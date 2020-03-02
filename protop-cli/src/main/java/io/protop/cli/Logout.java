package io.protop.cli;

import io.protop.core.logs.Logger;
import io.protop.core.logs.Logs;
import picocli.CommandLine.*;

@Command(name = "logout",
        description = "")
public class Logout implements Runnable {

    private static final Logger logger = Logger.getLogger(Logout.class);

    @ParentCommand
    private ProtopCli protop;

    @Override
    public void run() {
        Logs.enableIf(protop.isDebugMode());

        logger.info("logging out...");
    }
}
