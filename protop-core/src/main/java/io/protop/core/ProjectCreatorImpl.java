package io.protop.core;

import io.protop.core.error.ServiceError;
import io.protop.core.error.ServiceException;
import io.protop.core.logs.Logger;
import io.protop.core.manifest.Manifest;
import io.protop.core.storage.StorageService;
import lombok.AllArgsConstructor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@AllArgsConstructor
public class ProjectCreatorImpl implements ProjectCreator {

    private static Logger logger = Logger.getLogger(ProjectCreatorImpl.class);

    private final StorageService storageService;

    @Override
    public void create(Manifest manifest, Path directory) {

        Path configurationFilePath = Paths.get(directory.toString(), Manifest.PROTOP_JSON);

        if (Files.exists(configurationFilePath)) {
            throw new ServiceException(ServiceError.MANIFEST_ERROR, "Project already initialized.");
        }

        storageService.storeJson(manifest, configurationFilePath);
    }
}
