package io.protop.core.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.Strings;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.AbstractMap;
import java.util.Map;

@Getter
@EqualsAndHashCode
public class ProjectId {

    private static final String FULL_COLON_SEPARATOR = ":";

    private final String organization;
    private final String project;

    @JsonCreator
    ProjectId(String value) {
        this(splitNamesFrom(value));
    }

    public ProjectId(String organization, String project) {
        this.organization = organization;
        this.project = project;
    }

    ProjectId(Map.Entry<String, String> pair) {
        this.organization = pair.getKey();
        this.project = pair.getValue();
    }

    public static ProjectId from(String value) {
        return new ProjectId(value);
    }

    private static Map.Entry<String, String> splitNamesFrom(String value) {
        if (Strings.isNullOrEmpty(value)) {
            throw new InvalidDependencyName(value);
        }

        String[] values = new String[0];
        if (value.contains(FULL_COLON_SEPARATOR)) {
            values = value.split(FULL_COLON_SEPARATOR);
        }

        if (values.length != 2) {
            throw new InvalidDependencyName(value);
        }

        return new AbstractMap.SimpleImmutableEntry<>(values[0], values[1]);
    }

    @Override
    public String toString() {
        return String.join(FULL_COLON_SEPARATOR, organization, project);
    }
}
