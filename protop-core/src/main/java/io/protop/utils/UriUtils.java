package io.protop.utils;

import java.net.URI;
import java.net.URISyntaxException;
import com.google.common.base.Objects;
import org.apache.http.client.utils.URIBuilder;

import javax.validation.constraints.NotNull;

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

    public static URI fromString(String uriString) {
        return URI.create(removeTrailingSlash(uriString));
    }

    private static String removeTrailingSlash(String input) {
        if (input.length() > 1) {
            if (Objects.equal(input.charAt(input.length() - 1), '/')) {
                return input.substring(0, input.length() - 1);
            }
        }
        return input;
    }

    /**
     * Creates a new URI, adding the new segments to the original URI.
     */
    public static URI appendPathSegments(URI original, String... segments) throws URISyntaxException {
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
