package io.protop.core.manifest.converters;

import com.fasterxml.jackson.databind.util.StdConverter;
import io.protop.core.error.ServiceError;
import io.protop.core.error.ServiceException;
import io.protop.core.manifest.ProjectCoordinate;
import io.protop.core.manifest.ProjectVersionBuilder;
import io.protop.version.InvalidVersionString;
import io.protop.version.Version;

import java.util.HashMap;
import java.util.Map;

public class DependencyMapFromRaw extends StdConverter<Map<ProjectCoordinate, String>, Map<ProjectCoordinate, Version>> {

    @Override
    public Map<ProjectCoordinate, Version> convert(Map<ProjectCoordinate, String> value) {
        Map<ProjectCoordinate, Version> converted = new HashMap<>();
        value.entrySet().forEach(dep -> {
            try {
                converted.put(dep.getKey(), Version.valueOf(ProjectVersionBuilder.scheme, dep.getValue()));
            } catch (InvalidVersionString e) {
                throw new ServiceException(ServiceError.MANIFEST_ERROR, e);
            }
        });
        return converted;
    }
}
