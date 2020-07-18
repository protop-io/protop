package io.protop.cli;

import com.google.common.base.Strings;
import io.protop.cli.errors.ExceptionHandler;
import io.protop.core.Environment;
import io.protop.core.auth.AuthService;
import io.protop.core.auth.BasicAuthService;
import io.protop.core.auth.BasicCredentials;
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
        description = "(experimental) Login to the registry.")
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

    @Option(names = {"-u", "--user", "--username"},
            description = "username",
            required = false,
            arity = "0..1",
            defaultValue = "")
    private String username;

    @Option(names = {"-p", "--pass", "--password"},
            description = "password",
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

            URL registryUrl = new URL(Strings.isNullOrEmpty(registry)
                    ? Environment.UNIVERSAL_DEFAULT_REGISTRY
                    : registry);

            if (Strings.isNullOrEmpty(username)) {
                username = promptUsername(reader);
            }

            if (Strings.isNullOrEmpty(password)) {
                password = promptPassword(reader);
            }

            BasicCredentials basicCredentials = BasicCredentials.builder()
                    .registry(registryUrl)
                    .username(username)
                    .password(password)
                    .build();

            StorageService storageService = new StorageService();
            AuthService<BasicCredentials> authService = new BasicAuthService(storageService);

            authService.authorize(basicCredentials).blockingAwait();

            // TODO better message
            logger.always("Success!");
        });
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
