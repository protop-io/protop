package io.protop.core.manifest.converters;

import com.fasterxml.jackson.databind.util.StdConverter;
import io.protop.core.error.ServiceError;
import io.protop.core.error.ServiceException;
import io.protop.core.manifest.ProjectVersionBuilder;
import io.protop.version.InvalidVersionString;
import io.protop.version.Version;

public class StringToVersion extends StdConverter<String, Version> {

    @Override
    public Version convert(String value) {
        try {
            return Version.valueOf(ProjectVersionBuilder.scheme, value);
        } catch (InvalidVersionString e) {
            throw new ServiceException(ServiceError.MANIFEST_ERROR, e);
        }
    }
}
