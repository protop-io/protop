package io.protop.core.link;

import io.protop.core.manifest.Manifest;
import io.protop.core.manifest.ProjectCoordinate;
import io.protop.core.error.ServiceException;
import io.protop.core.logs.Logger;
import io.protop.core.publish.PublishableProject;
import io.protop.core.storage.Storage;
import io.protop.core.storage.StorageUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LinkService {

    Logger logger = Logger.getLogger(LinkService.class);

    public void link(PublishableProject project) {
        Manifest manifest = project.getManifest();
        String organization = manifest.getOrganization();
        String projectName = manifest.getName();

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

    public void Link(ProjectCoordinate other) {
        // TODO
    }

    public void Unlink(ProjectCoordinate other) {
        // TODO
    }
}
