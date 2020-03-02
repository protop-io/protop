package io.protop.core.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BasicCredentials implements Credentials {

    @NotNull
    @JsonProperty("registry")
    private final URI registry;

    @NotNull
    @JsonProperty("basic")
    private final String basic;

    @Valid
    @JsonCreator
    BasicCredentials(@JsonProperty("registry") URI registry,
                     @JsonProperty("basic") String basic) {
        this.registry = registry;
        this.basic = basic;
    }
}
