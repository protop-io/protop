package io.protop.cli;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import io.protop.calver.CalVer;
import io.protop.calver.InvalidVersionString;
import io.protop.core.ProjectCreator;
import io.protop.core.config.ProjectVersionBuilder;
import io.protop.core.error.ServiceError;
import io.protop.core.error.ServiceException;
import io.protop.core.logs.Logger;
import io.protop.core.logs.Logs;
import io.protop.core.ProjectCreatorImpl;
import io.protop.core.config.Configuration;
import io.protop.core.storage.StorageService;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.MaskingCallback;
import org.jline.reader.impl.DefaultParser;
import picocli.CommandLine.*;

@Command(name = "init",
        description = "")
public class Init implements Runnable {

    private static final Logger logger = Logger.getLogger(Init.class);
    private static final CalVer defaultVersion = ProjectVersionBuilder.newInstance()
            .major(1970)
            .minor(1)
            .micro(1)
            .modifier("SNAPSHOT")
            .build();
    private static final String CWD = ".";

    @ParentCommand
    private ProtopCli protop;

    @Parameters(arity = "0..1",
            description = "Root directory of project",
            defaultValue = ".")
    private Path directory;

    @Override
    public void run() {
        Logs.enableIf(protop.isDebugMode());

        logger.info("Creating a new project.");

        logger.info("default version is: " + defaultVersion.toString());

        LineReader reader = LineReaderBuilder.builder()
                .parser(new DefaultParser())
                .build();

        Configuration configuration = Configuration.builder()
                .organization(getOrganization(reader))
                .name(getName(reader))
                .version(getVersion(reader))
                .include(getPathsToInclude(reader))
                .build();

        createProject(configuration, directory);
    }

    private void createProject(Configuration configuration, Path directory) {
        StorageService storageService = new StorageService();
        ProjectCreator projectCreator = new ProjectCreatorImpl(storageService);

        try {
            logger.info("Creating new package manifest.");
            projectCreator.create(configuration, directory);

            logger.always(String.format("Initialized new project."));
        } catch (Exception e) {
            logger.always("Failed to create new project.");
            if (!Strings.isNullOrEmpty(e.getMessage()) && (e instanceof ServiceException)) {
                logger.always(e.getMessage());
            }
        }
    }

    private List<Path> getPathsToInclude(LineReader reader) {
        String leftPrompt = String.format("Entry point: (default \"%s\"): ", CWD);
        String rightPrompt = "";
        String pathString = reader.readLine(
                leftPrompt, rightPrompt, (MaskingCallback) null,null);
        if (Strings.isNullOrEmpty(pathString)) {
            pathString = CWD;
        }
        Path path = Paths.get(CWD).normalize().relativize(Paths.get(pathString));
        return ImmutableList.of(path);
    }

    private String getOrganization(LineReader reader) {
        String prompt = "Organization name (required): ";
        String rightPrompt = "";
        String name = reader.readLine(
                prompt, rightPrompt, (MaskingCallback) null,null);
        if (Strings.isNullOrEmpty(name)) {
            return getOrganization(reader);
        } else {
            return name;
        }
    }

    private String getName(LineReader reader) {
        String prompt = "Project name (required): ";
        String rightPrompt = "";
        String name = reader.readLine(
                prompt, rightPrompt, (MaskingCallback) null,null);
        if (Strings.isNullOrEmpty(name)) {
            return getName(reader);
        } else {
            return name;
        }
    }

    private CalVer getVersion(LineReader reader) {
        String prompt = String.format("Initial version (default %s): ", defaultVersion);
        String rightPrompt = "";
        String version = reader.readLine(
                prompt, rightPrompt, (MaskingCallback) null,null);
        if (Strings.isNullOrEmpty(version)) {
            return defaultVersion;
        } else {
            try {
                return CalVer.valueOf(ProjectVersionBuilder.scheme, version);
            } catch (InvalidVersionString e) {
                throw new ServiceException(ServiceError.CONFIGURATION_ERROR, e);
            }
        }
    }
}
