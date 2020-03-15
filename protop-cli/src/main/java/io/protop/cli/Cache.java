package io.protop.cli;

import io.protop.cli.errors.ExceptionHandler;
import io.protop.core.cache.CacheService;
import io.protop.core.logs.Logger;
import io.protop.core.logs.Logs;
import io.protop.core.storage.StorageService;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.MaskingCallback;
import org.jline.reader.impl.DefaultParser;
import picocli.CommandLine;

import java.util.Objects;

@CommandLine.Command(name = "cache",
        subcommands = {
                Cache.Clean.class
        },
        description = "Manipulate the system-wide cache.")
public class Cache implements Runnable {

    @CommandLine.ParentCommand
    private ProtopCli protop;

    public void run() {
        new CommandLine(new ProtopCli()).execute("help", "cache");
    }

    @CommandLine.Command(name = "clean",
            description = "Clean everything from the cache.")
    public static class Clean implements Runnable {

        private static final Logger logger = Logger.getLogger(Clean.class);

        @CommandLine.ParentCommand
        private Cache cache;

        public void run() {
            Logs.enableIf(cache.protop.isDebugMode());
            new ExceptionHandler().run(() -> {
                LineReader reader = LineReaderBuilder.builder()
                        .parser(new DefaultParser())
                        .build();

                if (confirm(reader)) {
                    StorageService storageService = new StorageService();
                    CacheService cacheService = new CacheService(storageService);
                    cacheService.clean()
                            .subscribe(this::handleSuccess, this::handleError)
                            .dispose();
                } else {
                    logger.always("Cancelled");
                }
            });
        }

        private void handleSuccess() {
            logger.always("Success!");
        }

        private void handleError(Throwable t) {
            logger.error("Failed to clean the cache.", t);
            if (!Logs.areEnabled()) {
                logger.always("Something went wrong. Try again with -d to enable debug logs.");
            }
        }

        private boolean confirm(LineReader reader) {
            String leftPrompt = String.format(
                    "This will delete all the cached dependencies in the system. " +
                            "Are you sure you want to do this? [y/n]: ");
            String rightPrompt = "";
            String answer = reader.readLine(
                    leftPrompt, rightPrompt, (MaskingCallback) null,null);
            return Objects.equals(answer, "y");
        }
    }
}
