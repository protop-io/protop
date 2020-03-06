package io.protop.core.auth;

import java.net.URI;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BasicCredentials implements Credentials {

    private final URI registry;
    private final String username;
    private final String password;
}
