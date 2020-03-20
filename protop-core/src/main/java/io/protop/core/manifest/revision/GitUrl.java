package io.protop.core.manifest.revision;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.validation.constraints.NotNull;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Getter
@EqualsAndHashCode
@AllArgsConstructor
public class GitUrl implements RevisionSource {

    @JsonValue
    @NotNull
    private final String raw;

    public static GitUrl fromRawPrefixedUrl(final String input) {
        if (!input.contains("git:")) {
            throw new InvalidRevision("Invalid Git URL. Must include the prefix: \"git:\"");
        }

        return fromRawUrl(input);
    }

    public static GitUrl fromRawUrl(final String url) {
        String rawUrl = url.replaceAll("git:", "");
        return new GitUrl(rawUrl);
    }

    public static GitUrl fromEncodedUrl(final String encodedUrl) {
        String decodedUrl = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8);
        return fromRawUrl(decodedUrl);
    }

    public String getUrlEncoded() {
        return URLEncoder.encode(raw, StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return raw;
    }
}
