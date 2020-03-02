package io.protop.cli;

import com.google.common.base.Strings;
import io.protop.core.auth.BasicCredentialService;
import io.protop.core.auth.CredentialService;
import io.protop.core.logs.Logger;
import io.protop.core.logs.Logs;
import io.protop.core.publishing.ProjectPublisher;
import io.protop.core.publishing.ProjectPublisherImpl;
import io.protop.core.publishing.PublishableProject;
import io.protop.core.registry.RegistryService;
import io.protop.core.registry.RegistryServiceImpl;
import io.protop.core.storage.StorageService;

import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;

import picocli.CommandLine.*;

@Command(name = "publish",
        description = "")
public class Publish implements Runnable {

    private static final Logger logger = Logger.getLogger(Publish.class);

    @ParentCommand
    private ProtopCli protop;

    @Parameters(arity = "0..1",
            description = "Location of project",
            defaultValue = ".")
    private Path location;

    @Option(names = {"-r", "--registry"},
            description = "Registry",
            required = false,
            arity = "0..1",
            defaultValue = "")
    private String registry;

    @Override
    public void run() {

        Logs.enableIf(protop.isDebugMode());

        RegistryService registryService = new RegistryServiceImpl();

        StorageService storageService = new StorageService();
        CredentialService credentialService = new BasicCredentialService(storageService);
        ProjectPublisher projectPublisher = new ProjectPublisherImpl(credentialService);

        PublishableProject project = PublishableProject.from(location);

        if (!Strings.isNullOrEmpty(registry)) {
            projectPublisher.publish(project, URI.create(registry));
        } else {
            URI publishRegistry = registryService.getDefaultRegistry();
            projectPublisher.publish(project, publishRegistry);
        }
    }
}
