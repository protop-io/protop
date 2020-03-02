package io.protop.cli;

import com.google.common.base.Strings;
import io.protop.core.Environment;
import io.protop.core.auth.*;
import io.protop.core.logs.Logger;
import io.protop.core.logs.Logs;
import io.protop.core.storage.StorageService;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.MaskingCallback;
import org.jline.reader.impl.DefaultParser;
import picocli.CommandLine.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;


@Command(name = "auth",
        description = "",
        subcommands = {
//                Auth.Forget.class
        })
public class Auth implements Runnable {

    private static final Logger logger = Logger.getLogger(Auth.class);

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

        LineReader reader = LineReaderBuilder.builder()
                .parser(new DefaultParser())
                .build();

        URI registryUri = Strings.isNullOrEmpty(registry)
                ? Environment.UNIVERSAL_DEFAULT_REGISTRY
                : URI.create(registry);

        if (Strings.isNullOrEmpty(username)) {
            username = promptUsername(reader);
        }

        if (Strings.isNullOrEmpty(password)) {
            password = promptPassword(reader);
        }

        String usernamePassword = String.join(":", username, password);
        String base64 = Base64.getEncoder().encodeToString(usernamePassword.getBytes());
        BasicCredentials basicCredentials = BasicCredentials.builder()
                .registry(registryUri)
                .basic(base64)
                .build();

        StorageService storageService = new StorageService();
        CredentialService credentialService = new BasicCredentialService(storageService);
        credentialService.use(basicCredentials)
            .blockingAwait();
        logger.always("Credentials stored.");
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

//    @Command(name = "forget",
//            description = "")
//    public static class Forget implements Runnable {
//
////        @ParentCommand
////        private ProtopCli protop;
//
//        @ParentCommand
//        private Auth auth;
//
//        @Override
//        public void run() {
//            if (Strings.isNullOrEmpty(auth.registry)) {
//                LineReader reader = LineReaderBuilder.builder()
//                        .parser(new DefaultParser())
//                        .build();
//
//            }
//        }
//
//        private URI promptRegistry(LineReader reader) {
//            String rightPrompt = "";
//            String registry = reader.readLine("registry (required): ", rightPrompt, (MaskingCallback) null,null);
//            if (Strings.isNullOrEmpty(registry)) {
//                return promptRegistry(reader);
//            } else {
//                return URI.create(registry);
//            }
//        }
//    }
}
