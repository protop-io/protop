package io.protop.core.manifest;

import io.protop.core.error.ServiceError;
import io.protop.core.error.ServiceException;
import io.protop.version.InvalidVersionString;
import io.protop.version.Scheme;
import io.protop.version.Version;

import java.util.Arrays;
import java.util.List;

public class ProjectVersionBuilder {

    public static final Scheme scheme = new Scheme(
            Scheme.Major.YYYY,
            Scheme.Minor.MM,
            Scheme.ANY_NUM);

    private int major;
    private int minor;
    private int micro;
    private String modifier;

    public ProjectVersionBuilder() {}

    public static ProjectVersionBuilder newInstance() {
        return new ProjectVersionBuilder();
    }

    public ProjectVersionBuilder major(int major) {
        this.major = major;
        return this;
    }

    public ProjectVersionBuilder minor(int minor) {
        this.minor = minor;
        return this;
    }

    public ProjectVersionBuilder micro(int micro) {
        this.micro = micro;
        return this;
    }

    public ProjectVersionBuilder modifier(String modifier) {
        this.modifier = modifier;
        return this;
    }

    public Version build() {
        List<String> segments = Arrays.asList(
                String.valueOf(major),
                String.valueOf(minor),
                String.valueOf(micro),
                modifier);

        try {
            return Version.valueOf(scheme, String.join(".", segments));
        } catch (InvalidVersionString e) {
            throw new ServiceException(ServiceError.MANIFEST_ERROR, e);
        }
    }
}
