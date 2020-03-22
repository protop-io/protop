package io.protop.core.manifest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.Strings;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.AbstractMap;
import java.util.Map;

@Getter
@EqualsAndHashCode
public class Coordinate {

    private static final String SLASH = "/";

    private final String organizationId;
    private final String projectId;

    @JsonCreator
    Coordinate(String value) {
        this(splitNamesFrom(value));
    }

    public Coordinate(String organizationId, String projectId) {
        this.organizationId = organizationId;
        this.projectId = projectId;
    }

    Coordinate(Map.Entry<String, String> pair) {
        this.organizationId = pair.getKey();
        this.projectId = pair.getValue();
    }

    public static Coordinate from(String value) {
        return new Coordinate(value);
    }

    private static Map.Entry<String, String> splitNamesFrom(String value) {
        if (Strings.isNullOrEmpty(value)) {
            throw new InvalidDependencyName(value);
        }

        String[] values = new String[0];
        if (value.contains(SLASH)) {
            values = value.split(SLASH);
        }

        if (values.length != 2) {
            throw new InvalidDependencyName(value);
        }

        return new AbstractMap.SimpleImmutableEntry<>(values[0], values[1]);
    }

    @Override
    public String toString() {
        return String.join(SLASH, organizationId, projectId);
    }
}
