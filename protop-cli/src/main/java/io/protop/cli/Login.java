package io.protop.cli;

import com.google.common.base.Strings;
import io.protop.cli.errors.ExceptionHandler;
import io.protop.core.Context;
import io.protop.core.Environment;
import io.protop.core.RuntimeConfiguration;
import io.protop.core.auth.AuthService;
import io.protop.core.auth.UserCredentials;
import io.protop.core.grpc.GrpcService;
import io.protop.core.logs.Logger;
import io.protop.core.logs.Logs;
import io.protop.core.storage.StorageService;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.MaskingCallback;
import org.jline.reader.impl.DefaultParser;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.net.URL;


@Command(name = "login",
        aliases = {"auth"},
        description = "Login to the registry.")
public class Login implements Runnable {

    private static final Logger logger = Logger.getLogger(Login.class);

    @ParentCommand
    private ProtopCli protop;

    @Option(names = {"-r", "--registry"},
            description = "Registry",
            required = false,
            arity = "0..1",
            defaultValue = "")
    private String registry;

    @Option(names = {"-u", "--username"},
            description = "Username",
            required = false,
            arity = "0..1",
            defaultValue = "")
    private String username;

    @Option(names = {"-p", "--password"},
            description = "Password",
            required = false,
            arity = "0..1",
            defaultValue = "")
    private String password;

    @Override
    public void run() {
        Logs.enableIf(protop.isDebugMode());
        new ExceptionHandler().run(() -> {
            LineReader reader = LineReaderBuilder.builder()
                    .parser(new DefaultParser())
                    .build();

            if (Strings.isNullOrEmpty(registry)) {
                registry = promptRegistry(reader);
            }

            if (Strings.isNullOrEmpty(username)) {
                username = promptUsername(reader);
            }

            if (Strings.isNullOrEmpty(password)) {
                password = promptPassword(reader);
            }

            URL registryUrl = new URL(Strings.isNullOrEmpty(registry)
                    ? Environment.UNIVERSAL_DEFAULT_REGISTRY
                    : registry);

            UserCredentials userCredentials = UserCredentials.builder()
                    .registry(registryUrl)
                    .username(username)
                    .password(password)
                    .build();

            RuntimeConfiguration cliRc = RuntimeConfiguration.builder()
                    .repositoryUrl(registry)
                    .username(username)
                    .password(password)
                    .build();
            Context context = Context.from(cliRc);

            StorageService storageService = new StorageService();
            GrpcService grpcService = new GrpcService();
            AuthService authService = new AuthService(storageService, grpcService, context);

            try {
                authService.authorize(userCredentials).blockingGet();
                // TODO better message
                logger.always("Success!");
            } catch (Throwable t) {
                logger.error("Something went wrong; failed to authorize.", t);
                throw t;
            }
        });
    }

    private String promptRegistry(LineReader reader) {
        String rightPrompt = "";
        registry = reader.readLine("registry (required): ", rightPrompt, (MaskingCallback) null,null);
        if (Strings.isNullOrEmpty(registry)) {
            return promptRegistry(reader);
        } else {
            return registry;
        }
    }

    private String promptUsername(LineReader reader) {
        String rightPrompt = "";
        username = reader.readLine("username (required): ", rightPrompt, (MaskingCallback) null,null);
        if (Strings.isNullOrEmpty(username)) {
            return promptUsername(reader);
        } else {
            return username;
        }
    }

    private String promptPassword(LineReader reader) {
        String rightPrompt = "";
        char[] password = reader.readLine("password (required): ", rightPrompt, (Character) '*', null)
                .toCharArray();
        if (password.length == 0) {
            return promptPassword(reader);
        } else {
            return new String(password);
        }
    }
}
