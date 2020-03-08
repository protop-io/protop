package io.protop.core.manifest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.protop.core.logs.Logger;
import io.protop.core.manifest.converters.StringManifestMapToVersionManifestMap;
import io.protop.core.manifest.converters.StringToVersion;
import io.protop.core.manifest.converters.VersionToString;
import io.protop.version.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.io.FileUtils;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Getter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
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
    private final Map<Version, Manifest> versions;

    @NotNull
    @JsonProperty
    private final String readme;

    @NotNull
    @JsonProperty("_attachments")
    private final Map<String, Attachment> attachments;

    @JsonCreator
    AggregatedManifest(@JsonProperty("_id")
                               String id,
                       @JsonProperty("org")
                               String org,
                       @JsonProperty("name")
                               String name,
                       @JsonProperty("version")
                       @JsonDeserialize(converter = StringToVersion.class)
                               Version version,
                       @JsonProperty("description")
                               String description,
                       @JsonProperty("dist-tags")
                       @JsonDeserialize(as = HashMap.class)
                               Map<String, String> distTags,
                       @JsonProperty("versions")
                       @JsonDeserialize(converter = StringManifestMapToVersionManifestMap.class)
                               Map<Version, Manifest> versions,
                       @JsonProperty("readme")
                               String readme,
                       @JsonProperty("_attachments")
                       @JsonDeserialize(as = HashMap.class)
                               Map<String, Attachment> attachments) {
        this.id = id;
        this.org = org;
        this.name = name;
        this.version = version;
        this.description = description;
        this.distTags = distTags;
        this.versions = versions;
        this.readme = readme;
        this.attachments = attachments;
    }

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
