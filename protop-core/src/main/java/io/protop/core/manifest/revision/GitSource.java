package io.protop.core.manifest.revision;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.validation.constraints.NotNull;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@EqualsAndHashCode
@AllArgsConstructor
public class GitSource implements RevisionSource {

    private static final String AT = "@";

    @JsonValue
    @NotNull
    private final String raw;

    public static GitSource fromRawPrefixedUrl(final String input) {
        for (Platform platform : Platform.values()) {
            if (platform.prefixes(input)) {
                return new GitSource(platform.rawUrlFrom(input));
            }
        }

        throw new InvalidRevision(
                "Invalid Git URL. Must include one of the valid prefixes: " +
                Arrays.stream(Platform.values())
                        .map(Platform::getPrefix)
                        .collect(Collectors.joining(",")));
    }

    public static GitSource fromEncodedUrl(final String encodedUrl) {
        String decodedUrl = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8);
        return new GitSource(decodedUrl);
    }

    // This is mainly useful for using the URL as a path in the file system.
    public String getUrlEncoded() {
        return URLEncoder.encode(raw, StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return raw;
    }

    @Getter
    @AllArgsConstructor
    private enum Platform {

        // Any valid git repository; the input should be the full URL
        GIT("Git", "git:", source -> {
            String[] args = source.split(AT);
            return args[0];
        }),

        // Github
        GITHUB("Github", "gh:", source -> {
            String[] args = source.split(AT);
            String path = args[0];
            return String.format("https://github.com/%s", path);
        }),

        // Gitlab
        GITLAB("Gitlab", "gl:", source -> {
            String[] args = source.split(AT);
            String path = args[0];
            return String.format("https://gitlab.com/%s", path);
        }),

        // Bitbucket
        BITBUCKET("Bitbucket", "bb:", source -> {
            String[] args = source.split(AT);
            String path = args[0];
            return String.format("https://bitbucket.org/%s", path);
        });

        private final String name;
        private final String prefix;
        private final Function<String, String> urlProducer;

        public boolean prefixes(final String prefixedInput) {
            return prefixedInput.startsWith(prefix);
        }

        private String removePrefix(final String prefixedInput) {
            // This may be a redundant check in some uses, but this is to avoid
            // something like a bad input slipping through the cracks anywhere.
            if (prefixes(prefixedInput)) {
                return prefixedInput.replaceFirst(prefix, "");
            } else {
                throw new InvalidRevision(String.format(
                        "Invalid input cannot be parsed as a %s source: %s",
                        name, prefixedInput));
            }
        }

        public String rawUrlFrom(final String prefixedInput) {
            return urlProducer.apply(removePrefix(prefixedInput));
        }
    }
}
