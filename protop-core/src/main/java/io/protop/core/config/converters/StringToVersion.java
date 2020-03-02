package io.protop.core.config.converters;

import com.fasterxml.jackson.databind.util.StdConverter;
import io.protop.calver.CalVer;
import io.protop.calver.InvalidVersionString;
import io.protop.core.config.ProjectVersionBuilder;
import io.protop.core.error.ServiceError;
import io.protop.core.error.ServiceException;

public class StringToVersion extends StdConverter<String, CalVer> {

    @Override
    public CalVer convert(String value) {
        try {
            return CalVer.valueOf(ProjectVersionBuilder.scheme, value);
        } catch (InvalidVersionString e) {
            throw new ServiceException(ServiceError.CONFIGURATION_ERROR, e);
        }
    }
}
