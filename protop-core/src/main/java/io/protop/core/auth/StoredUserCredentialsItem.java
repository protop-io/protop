package io.protop.core.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.net.URL;

@Getter
@Builder
public class StoredUserCredentialsItem {

    @JsonProperty
    private final URL registry;

    @JsonProperty
    private final String token;

    @JsonProperty
    private final String username;

    @JsonProperty
    private final String password;

    @JsonProperty
    private final long timestamp;

    @JsonProperty
    private final long ttl;

    @JsonCreator
    StoredUserCredentialsItem(@JsonProperty("registry") URL registry,
                              @JsonProperty("token") String token,
                              @JsonProperty("username") String username,
                              @JsonProperty("password") String password,
                              @JsonProperty("timestamp") long timestamp,
                              @JsonProperty("ttl") long ttl) {
        this.registry = registry;
        this.token = token;
        this.username = username;
        this.password = password;
        this.timestamp = timestamp;
        this.ttl = ttl;
    }

    @JsonIgnore
    public boolean isExpired() {
        return (ttl < 1) ||
                (timestamp < 1) ||
                (System.currentTimeMillis() - timestamp > ttl);
    }

    public static StoredUserCredentialsItem of(UserCredentials userCredentials) {
        return StoredUserCredentialsItem.builder()
                .registry(userCredentials.getRegistry())
                .username(userCredentials.getUsername())
                .password(userCredentials.getPassword())
                .ttl(0)
                .build();
    }
}
