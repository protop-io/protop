package io.protop.core.publishing;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Map;

import io.protop.calver.CalVer;
import lombok.AllArgsConstructor;
import lombok.Builder;

import javax.validation.constraints.NotNull;

@Builder
public class Manifest {

    @NotNull
    @JsonProperty("_id")
    private final String id;

    @NotNull
    private final String org;

    @NotNull
    private final String name;

    @NotNull
    private final CalVer version;

    @NotNull
    private final String description;

    @NotNull
    @JsonProperty("dist-tags")
    private final Map<String, String> distTags;

    @NotNull
    private final Map<String, Object> versions;

    @NotNull
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
        private final String data;

        @NotNull
        private final int length;

        public static Attachment of(File file) throws IOException {
            String fileString = Files.readString(file.toPath());
            return new Attachment(
                    "application/octet-stream",
                    new String(Base64.getEncoder().encode(fileString.getBytes())),
                    fileString.length());
        }
    }
}
