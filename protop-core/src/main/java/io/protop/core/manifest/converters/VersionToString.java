package io.protop.core.manifest.converters;

import com.fasterxml.jackson.databind.util.StdConverter;
import io.protop.version.Version;

public class VersionToString extends StdConverter<Version, String> {

    @Override
    public String convert(Version value) {
        return value.toString();
    }
}
