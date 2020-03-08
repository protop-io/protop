package io.protop.core.manifest.converters;

import com.fasterxml.jackson.databind.util.StdConverter;
import io.protop.core.error.ServiceError;
import io.protop.core.error.ServiceException;
import io.protop.core.manifest.Manifest;
import io.protop.core.manifest.ProjectVersionBuilder;
import io.protop.version.InvalidVersionString;
import io.protop.version.Version;

import java.util.HashMap;
import java.util.Map;

public class StringManifestMapToVersionManifestMap extends StdConverter<Map<String, Manifest>, Map<Version, Manifest>> {

    @Override
    public Map<Version, Manifest> convert(Map<String, Manifest> value) {
        Map<Version, Manifest> output = new HashMap<>();
        value.forEach((s, manifest) -> {
            try {
                output.put(Version.valueOf(ProjectVersionBuilder.scheme, s), manifest);
            } catch (InvalidVersionString ivs) {
                throw new ServiceException(ServiceError.MANIFEST_ERROR, "Invalid version: " + s);
            }
        });
        return output;
    }
}
