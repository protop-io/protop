package io.protop.core.auth;

import lombok.Builder;
import lombok.Getter;

import java.net.URL;

@Getter
@Builder
public class BasicCredentials implements Credentials {

    private final URL registry;
    private final String username;
    private final String password;
}
