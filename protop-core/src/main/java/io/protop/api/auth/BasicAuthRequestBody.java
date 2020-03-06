package io.protop.api.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public class BasicAuthRequestBody {

    @JsonProperty
    private final String username;

    @JsonProperty
    private final String password;
}
