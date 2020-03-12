package io.protop.utils;

import io.protop.core.manifest.ProjectCoordinate;
import io.protop.core.version.Version;

import java.net.URI;
import java.net.URISyntaxException;

public class RegistryUtils {

    private RegistryUtils() {
        // no op
    }

    public static String createTarballName(ProjectCoordinate coordinate, Version version) {
        return String.join("-",
                coordinate.getOrganizationId(),
                coordinate.getProjectId(),
                version.toString())
                + ".tar.gz";
    }

    public static URI createTarballUri(URI registryUri, ProjectCoordinate coordinate, Version version)
            throws URISyntaxException {
        return UriUtils.appendPathSegments(
                registryUri,
                coordinate.getOrganizationId(),
                coordinate.getProjectId(),
                "-",
                RegistryUtils.createTarballName(coordinate, version));
    }

    public static URI createManifestUri(URI registryUri, ProjectCoordinate coordinate) throws URISyntaxException {
        return UriUtils.appendPathSegments(
                registryUri,
                coordinate.getOrganizationId(),
                coordinate.getProjectId());
    }
}
