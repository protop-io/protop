package io.protop.core.link;

import io.protop.core.config.Configuration;
import io.protop.core.config.ProjectId;
import io.protop.core.error.ServiceException;
import io.protop.core.logs.Logger;
import io.protop.core.publishing.PublishableProject;
import io.protop.core.storage.Storage;
import io.protop.core.storage.StorageUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LinkService {

    Logger logger = Logger.getLogger(LinkService.class);

    public void link(PublishableProject project) {
        Configuration configuration = project.getConfiguration();
        String organization = configuration.getOrganization();
        String projectName = configuration.getName();

        Path linksDirectory = Storage.pathOf(Storage.GlobalDirectory.LINKS);

        try {
            Path orgPath = linksDirectory.resolve(organization);
            StorageUtils.createDirectoryIfNotExists(orgPath);

            Path projectPath = orgPath.resolve(projectName);
            StorageUtils.createDirectoryIfNotExists(projectPath);

            if (Files.exists(projectPath)) {
                Files.delete(projectPath);
            }
            Files.createSymbolicLink(projectPath, project.getProjectLocation());
        } catch (IOException e) {
            logger.error("Failed to link project.", e);
            throw new ServiceException("Failed to link project.", e);
        }
    }

    public void Unlink(PublishableProject project) {
        // TODO
    }

    public void Link(ProjectId other) {
        // TODO
    }

    public void Unlink(ProjectId other) {
        // TODO
    }
}
