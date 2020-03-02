package io.protop.core.config.converters;

import com.fasterxml.jackson.databind.util.StdConverter;
import io.protop.calver.CalVer;

public class VersionToString extends StdConverter<CalVer, String> {

    @Override
    public String convert(CalVer value) {
        return value.toString();
    }
}
