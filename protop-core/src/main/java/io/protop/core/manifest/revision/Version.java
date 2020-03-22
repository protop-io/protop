package io.protop.core.manifest.revision;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.EqualsAndHashCode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 *
 * {@see https://semver.org/#backusnaur-form-grammar-for-valid-semver-versions}
 */
@EqualsAndHashCode
public class Version implements RevisionSource, Comparable<Version> {

    private static final String PATTERN = "^(?<major>0|[1-9]\\d*)\\.(?<minor>0|[1-9]\\d*)\\.(?<patch>0|[1-9]\\d*)" +
            "(?:-(?<prerelease>(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)" +
            "(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?" +
            "(?:\\+(?<buildmetadata>[0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$";
    private static final Pattern pattern = Pattern.compile(PATTERN);

    @JsonValue
    private final String value;

    public Version(final String value) {
        Matcher matcher = pattern.matcher(value);
        if (matcher.matches()) {
            this.value = value;
        } else {
            throw new InvalidVersionString(value);
        }
    }

    @Override
    public int compareTo(Version o) {
        return value.compareTo(o.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
