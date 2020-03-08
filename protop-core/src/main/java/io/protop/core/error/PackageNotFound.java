package io.protop.core.error;

import io.protop.core.manifest.ProjectCoordinate;
import io.protop.version.Version;

public class PackageNotFound extends ServiceException {

    public PackageNotFound(ProjectCoordinate coordinate, Version version) {
        super(ServiceError.PACKAGE_NOT_FOUND, String.format(
                "Could not find %s %s.",
                coordinate,
                version));
    }
}
