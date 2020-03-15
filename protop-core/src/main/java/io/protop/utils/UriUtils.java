package io.protop.utils;

import com.google.common.base.Strings;
import org.apache.http.client.utils.URIBuilder;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public class UriUtils {

    private UriUtils() {
        // no op
    }

    /**
     * Returns <code>URI.toString()</code>, removing a trailing '/' if present.
     */
    public static String toString(@NotNull URI uri) {
        return removeTrailingSlash(uri.toString());
    }

    @Nullable
    public static URI fromString(@Nullable String uriString) {
        String resolvedUriString = removeTrailingSlash(uriString);
        if (Strings.isNullOrEmpty(resolvedUriString)) {
            return null;
        }
        return URI.create(resolvedUriString);
    }

    @Nullable
    private static String removeTrailingSlash(@Nullable String input) {
        if (Strings.isNullOrEmpty(input)) {
            return null;
        }
        if (input.length() > 1) {
            if (Objects.equals(input.charAt(input.length() - 1), '/')) {
                return input.substring(0, input.length() - 1);
            }
        }
        return input;
    }

    /**
     * Creates a new URI, adding the new segments to the original URI.
     */
    public static URI appendPathSegments(@NotNull URI original, String... segments) throws URISyntaxException {
        String originalPath = removeTrailingSlash(original.getPath());
        String fullPath = segments.length > 0
                ? originalPath + "/" + String.join("/", segments)
                : originalPath;
        String resolvedFullPath = fullPath.replaceAll("//", "/");
        return new URIBuilder(original)
                .setHost(original.getHost())
                .setPath(resolvedFullPath)
                .build();
    }
}
