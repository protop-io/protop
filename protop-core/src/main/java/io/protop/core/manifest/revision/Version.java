package io.protop.core.manifest.revision;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.EqualsAndHashCode;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@EqualsAndHashCode
public class Version implements RevisionSource, Comparable<Version> {

    private static final String PATTERN = "^([0-9]+)\\.([0-9]+)\\.([0-9]+)(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?(?:\\+[0-9A-Za-z-]+)?$";
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

    public static Version of(String value) {
        return new Version(value);
    }

    @Override
    public int compareTo(Version o) {
        return new ComparableVersion(value).compareTo(new ComparableVersion(o.value));
    }

    @Override
    public String toString() {
        return value;
    }
}
