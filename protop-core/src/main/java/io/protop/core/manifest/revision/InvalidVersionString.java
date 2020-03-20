package io.protop.core.manifest.revision;

public class InvalidVersionString extends RuntimeException {

    public InvalidVersionString(final String versionString) {
        super(String.format("This does not match a valid revision pattern: %s.",
                versionString));
    }
}
