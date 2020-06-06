package io.protop.core.manifest.revision;

import com.google.common.base.Strings;

import java.util.Optional;

public class RevisionUtils {

    private RevisionUtils() {
        // no op
    }

    public static RevisionSource fromString(final String input) {
        Optional<GitSource> gitUrl = mapStringToGitUrl(input);
        if (gitUrl.isPresent()) {
            return gitUrl.get();
        }

        Optional<Version> version = mapStringToVersion(input);
        if (version.isPresent()) {
            return version.get();
        }

        throw new InvalidRevision("Entry could not be mapped to a valid version or Git path: " + input);

    }

    private static Optional<GitSource> mapStringToGitUrl(final String input) {
        if (Strings.isNullOrEmpty(input)){
            return Optional.empty();
        }

        try {
            GitSource gitSource = GitSource.fromRawPrefixedInput(input);
            return Optional.of(gitSource);
        } catch (InvalidRevision invalidRevision) {
            return Optional.empty();
        }
    }

    private static Optional<Version> mapStringToVersion(final String input) {
        try {
            Version version = new Version(input);
            return Optional.of(version);
        } catch (InvalidVersionString invalidVersionString) {
            return Optional.empty();
        }
    }
}
