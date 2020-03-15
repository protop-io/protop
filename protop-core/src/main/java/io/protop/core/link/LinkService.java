package io.protop.core.link;

import io.protop.core.error.ServiceException;
import io.protop.core.logs.Logger;
import io.protop.core.manifest.Manifest;
import io.protop.core.manifest.ProjectCoordinate;
import io.protop.core.publish.PublishableProject;
import io.protop.core.storage.Storage;
import io.protop.core.storage.StorageUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LinkService {

    Logger logger = Logger.getLogger(LinkService.class);

    public void link(PublishableProject project) {
        try {
            ProjectCoordinate coordinate = getCoordinate(project);
            Path linkPath = getPathToLink(coordinate);
            Files.deleteIfExists(linkPath);
            Files.createSymbolicLink(linkPath, project.getProjectLocation());
        } catch (IOException e) {
            logger.error("Failed to link project.", e);
            throw new ServiceException("Failed to link project.", e);
        }
    }

    public void unlink(PublishableProject project) {
        try {
            ProjectCoordinate coordinate = getCoordinate(project);
            Path linkPath = getPathToLink(coordinate);
            Files.deleteIfExists(linkPath);
        } catch (IOException e) {
            logger.error("Failed to unlink project.", e);
            throw new ServiceException("Failed to unlink project.", e);
        }
    }

    public void clean() throws IOException {
        Path linksDirectory = getLinksDirectory();
        Files.list(linksDirectory).forEach(path -> path.toFile().delete());
    }

    private Path getPathToLink(ProjectCoordinate coordinate) throws IOException {
        Path linksDirectory = getLinksDirectory();

        Path orgPath = linksDirectory.resolve(coordinate.getOrganizationId());
        StorageUtils.createDirectoryIfNotExists(orgPath);

        return orgPath.resolve(coordinate.getProjectId());
    }

    private ProjectCoordinate getCoordinate(PublishableProject project) {
        Manifest manifest = project.getManifest();
        return new ProjectCoordinate(manifest.getOrganization(), manifest.getName());
    }

    private Path getLinksDirectory() {
        return Storage.pathOf(Storage.GlobalDirectory.LINKS);
    }
}
