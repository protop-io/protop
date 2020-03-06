package io.protop.core.publish;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.protop.core.manifest.converters.VersionToString;
import io.protop.core.logs.Logger;
import io.protop.version.Version;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.apache.commons.io.FileUtils;

import javax.validation.constraints.NotNull;

@Builder
public class AggregatedManifest {

    private static final Logger logger = Logger.getLogger(AggregatedManifest.class);

    @NotNull
    @JsonProperty("_id")
    private final String id;

    @NotNull
    @JsonProperty
    private final String org;

    @NotNull
    @JsonProperty
    private final String name;

    @NotNull
    @JsonProperty
    @JsonSerialize(converter = VersionToString.class)
    private final Version version;

    @NotNull
    @JsonProperty
    private final String description;

    @NotNull
    @JsonProperty("dist-tags")
    private final Map<String, String> distTags;

    @NotNull
    @JsonProperty
    private final Map<String, Object> versions;

    @NotNull
    @JsonProperty
    private final String readme;

    @NotNull
    @JsonProperty("_attachments")
    private final Map<String, Attachment> attachments;

    @AllArgsConstructor
    public static class Attachment {

        @NotNull
        @JsonProperty("content_type")
        private final String contentType;

        @NotNull
        @JsonProperty
        private final String data;

        @NotNull
        @JsonProperty
        private final int length;

        public static Attachment of(File file) throws IOException {
            byte[] fileBytes = FileUtils.readFileToByteArray(file);

            return new Attachment(
                    "application/octet-stream",
                    new String(Base64.getEncoder().encode(fileBytes)),
                    fileBytes.length);
        }
    }
}
