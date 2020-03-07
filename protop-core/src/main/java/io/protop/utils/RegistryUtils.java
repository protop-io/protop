package io.protop.utils;

import io.protop.core.manifest.ProjectCoordinate;
import io.protop.version.Version;

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
}
