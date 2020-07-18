package io.protop.core.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.net.URL;

@Getter
@Builder
public class AuthToken {

    @JsonProperty
    private final URL registry;

    @JsonProperty
    private final String value;

    @JsonCreator
    AuthToken(@JsonProperty("registry") URL registry,
              @JsonProperty("value") String value) {
        this.registry = registry;
        this.value = value;
    }
}
