package io.protop.core.publish;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.protop.core.manifest.Manifest;
import lombok.Builder;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotNull;

@SuperBuilder
public class PublishableManifest extends Manifest {

    @NotNull
    @JsonProperty("dist")
    private final Dist dist;

    @Builder
    public static class Dist {

        @JsonProperty
        private final String integrity;

        @JsonProperty
        private final String shasum;

        @JsonProperty
        private final long fileCount;

        @JsonProperty
        private final long unpackedSize;

        @JsonProperty("protop-signature")
        private final String signature;

        @JsonProperty
        private final String tarball;
    }
}
