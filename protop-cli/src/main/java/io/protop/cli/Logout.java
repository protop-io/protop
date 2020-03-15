package io.protop.cli;

import com.google.common.base.Strings;
import io.protop.cli.errors.ExceptionHandler;
import io.protop.core.auth.AuthService;
import io.protop.core.auth.BasicAuthService;
import io.protop.core.logs.Logger;
import io.protop.core.logs.Logs;
import io.protop.core.storage.StorageService;
import io.protop.utils.UriUtils;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.MaskingCallback;
import org.jline.reader.impl.DefaultParser;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.net.URI;

@Command(name = "logout",
        aliases = {"forget"},
        description = "Remove credentials for the registry.")
public class Logout implements Runnable {

    private static final Logger logger = Logger.getLogger(Logout.class);

    @ParentCommand
    private ProtopCli protop;

    @Option(names = {"-r", "--registry"},
            description = "Registry",
            required = false,
            arity = "0..1",
            defaultValue = "")
    private String registry;

    @Override
    public void run() {
        Logs.enableIf(protop.isDebugMode());
        new ExceptionHandler().run(() -> {
            if (Strings.isNullOrEmpty(registry)) {
                LineReader reader = LineReaderBuilder.builder()
                        .parser(new DefaultParser())
                        .build();
                URI registryUri = promptRegistry(reader);

                StorageService storageService = new StorageService();
                AuthService<?> authService = new BasicAuthService(storageService);
                authService.forget(registryUri).blockingAwait();
                handleSuccess();
            }
        });
    }

    private void handleSuccess() {
        logger.always("Success!");
    }

    private URI promptRegistry(LineReader reader) {
        String rightPrompt = "";
        String registry = reader.readLine("registry (required): ", rightPrompt, (MaskingCallback) null,null);
        if (Strings.isNullOrEmpty(registry)) {
            return promptRegistry(reader);
        } else {
            return UriUtils.fromString(registry);
        }
    }
}
