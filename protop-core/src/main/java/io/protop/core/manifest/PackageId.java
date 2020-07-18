package io.protop.core.manifest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.Strings;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.AbstractMap;
import java.util.Map;

@Getter
@EqualsAndHashCode
public class PackageId {

    private static final String SLASH = "/";

    private final String organization;
    private final String project;

    @JsonCreator
    PackageId(String value) {
        this(splitNamesFrom(value));
    }

    public PackageId(String organization, String project) {
        this.organization = organization;
        this.project = project;
    }

    PackageId(Map.Entry<String, String> pair) {
        this.organization = pair.getKey();
        this.project = pair.getValue();
    }

    public static PackageId from(String value) {
        return new PackageId(value);
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
        return String.join(SLASH, organization, project);
    }
}
