package io.protop.core.auth;

import lombok.Builder;
import lombok.Getter;

import java.net.URI;

@Getter
@Builder
public class BasicCredentials implements Credentials {

    private final URI registry;
    private final String username;
    private final String password;
}
