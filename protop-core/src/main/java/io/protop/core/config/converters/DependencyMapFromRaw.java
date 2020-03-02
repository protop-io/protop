package io.protop.core.config.converters;

import com.fasterxml.jackson.databind.util.StdConverter;
import io.protop.calver.CalVer;
import io.protop.calver.InvalidVersionString;
import io.protop.core.config.ProjectId;
import io.protop.core.config.ProjectVersionBuilder;
import io.protop.core.error.ServiceError;
import io.protop.core.error.ServiceException;

import java.util.HashMap;
import java.util.Map;

public class DependencyMapFromRaw extends StdConverter<Map<ProjectId, String>, Map<ProjectId, CalVer>> {

    @Override
    public Map<ProjectId, CalVer> convert(Map<ProjectId, String> value) {
        Map<ProjectId, CalVer> converted = new HashMap<>();
        value.entrySet().forEach(dep -> {
            try {
                converted.put(dep.getKey(), CalVer.valueOf(ProjectVersionBuilder.scheme, dep.getValue()));
            } catch (InvalidVersionString e) {
                throw new ServiceException(ServiceError.CONFIGURATION_ERROR, e);
            }
        });
        return converted;
    }
}
