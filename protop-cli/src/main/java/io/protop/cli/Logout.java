package io.protop.cli;

import com.google.common.base.Strings;
import io.protop.core.logs.Logger;
import io.protop.core.logs.Logs;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.MaskingCallback;
import org.jline.reader.impl.DefaultParser;
import picocli.CommandLine.*;

import java.net.URI;

@Command(name = "logout",
        aliases = {"forget"},
        description = "Remove/forget credentials for the registry.")
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

        if (Strings.isNullOrEmpty(registry)) {
            LineReader reader = LineReaderBuilder.builder()
                    .parser(new DefaultParser())
                    .build();
            URI registryUri = promptRegistry(reader);
        }
    }

    private URI promptRegistry(LineReader reader) {
            String rightPrompt = "";
            String registry = reader.readLine("registry (required): ", rightPrompt, (MaskingCallback) null,null);
            if (Strings.isNullOrEmpty(registry)) {
                return promptRegistry(reader);
            } else {
                return URI.create(registry);
            }
        }
}
