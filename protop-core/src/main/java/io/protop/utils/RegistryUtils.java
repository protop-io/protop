package io.protop.utils;

import io.protop.core.manifest.Coordinate;
import io.protop.core.manifest.revision.Version;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;

public class RegistryUtils {

    private RegistryUtils() {
        // no op
    }

    public static String createTarballName(Coordinate coordinate, Version version) {
        return String.join("-",
                coordinate.getOrganizationId(),
                coordinate.getProjectId(),
                version.toString())
                + ".tar.gz";
    }

    public static URI createTarballUri(@NotNull URI registryUri, Coordinate coordinate, Version version)
            throws URISyntaxException {
        return UriUtils.appendPathSegments(
                registryUri,
                coordinate.getOrganizationId(),
                coordinate.getProjectId(),
                "-",
                RegistryUtils.createTarballName(coordinate, version));
    }

    public static URI createManifestUri(@NotNull URI registryUri, Coordinate coordinate) throws URISyntaxException {
        return UriUtils.appendPathSegments(
                registryUri,
                coordinate.getOrganizationId(),
                coordinate.getProjectId());
    }
}
