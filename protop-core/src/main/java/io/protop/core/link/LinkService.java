package io.protop.core.link;

import io.protop.core.logs.Logger;
import io.protop.core.manifest.Manifest;
import io.protop.core.manifest.Coordinate;
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
            Coordinate coordinate = getCoordinate(project);
            Path linkPath = getPathToLink(coordinate);
            Files.deleteIfExists(linkPath);
            Files.createSymbolicLink(linkPath, project.getProjectLocation());
        } catch (IOException e) {
            String message = "Failed to link project.";
            logger.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    public void unlink(PublishableProject project) {
        try {
            Coordinate coordinate = getCoordinate(project);
            Path linkPath = getPathToLink(coordinate);
            Files.deleteIfExists(linkPath);
        } catch (IOException e) {
            String message = "Failed to unlink project.";
            logger.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    public void clean() throws IOException {
        Path linksDirectory = getLinksDirectory();
        Files.list(linksDirectory).forEach(path -> path.toFile().delete());
    }

    private Path getPathToLink(Coordinate coordinate) throws IOException {
        Path linksDirectory = getLinksDirectory();

        Path orgPath = linksDirectory.resolve(coordinate.getOrganizationId());
        StorageUtils.createDirectoryIfNotExists(orgPath);

        return orgPath.resolve(coordinate.getProjectId());
    }

    private Coordinate getCoordinate(PublishableProject project) {
        Manifest manifest = project.getManifest();
        return new Coordinate(manifest.getOrganization(), manifest.getName());
    }

    private Path getLinksDirectory() {
        return Storage.pathOf(Storage.GlobalDirectory.LINKS);
    }
}
