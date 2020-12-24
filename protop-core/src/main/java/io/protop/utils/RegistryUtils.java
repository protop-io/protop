package io.protop.utils;

import io.protop.core.manifest.PackageId;
import io.protop.core.manifest.revision.Version;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class RegistryUtils {

    private RegistryUtils() {
        // no op
    }

    public static String createTarballName(PackageId packageId, Version version) {
        return String.join("-",
                packageId.getOrganization(),
                packageId.getProject(),
                version.toString())
                + ".tar.gz";
    }

    public static URI createTarballUri(@NotNull URI registryUri, PackageId packageId, Version version)
            throws URISyntaxException {
        return UriUtils.appendPathSegments(
                registryUri,
                packageId.getOrganization(),
                packageId.getProject(),
                "-",
                RegistryUtils.createTarballName(packageId, version));
    }

    public static URI createManifestUri(@NotNull URL registryUrl, PackageId packageId) throws URISyntaxException {
        return UriUtils.appendPathSegments(
                registryUrl.toURI(),
                packageId.getOrganization(),
                packageId.getProject());
    }
}
