package io.protop.core.auth;

import lombok.Builder;
import lombok.Getter;

import java.net.URL;

@Getter
@Builder
public class UserCredentials {

    private final URL registry;
    private final String username;
    private final String password;

    public static UserCredentials of(StoredUserCredentialsItem storedUserCredentialsItem) {
        return UserCredentials.builder()
                .registry(storedUserCredentialsItem.getRegistry())
                .username(storedUserCredentialsItem.getUsername())
                .password(storedUserCredentialsItem.getPassword())
                .build();
    }
}
