package io.protop.core;

import io.protop.core.config.Configuration;
import io.protop.core.error.ServiceError;
import io.protop.core.error.ServiceException;
import io.protop.core.logs.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.protop.core.storage.StorageService;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ProjectCreatorImpl implements ProjectCreator {

    private static Logger logger = Logger.getLogger(ProjectCreatorImpl.class);

    private final StorageService storageService;

    @Override
    public void create(Configuration configuration, Path directory) {

        Path configurationFilePath = Paths.get(directory.toString(), Configuration.FILE_NAME);

        if (Files.exists(configurationFilePath)) {
            throw new ServiceException(ServiceError.CONFIGURATION_ERROR, "Project already initialized.");
        }

        storageService.storeJson(configuration, configurationFilePath);
    }
}
