package io.protop.core.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.protop.core.logs.Logger;
import lombok.Builder;
import lombok.Getter;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Contains all saved credentials mapping to registries. Multiple credentials are not allowed for a single registry.
 */
@Getter
@Builder
public class CredentialStore {

    private static final Logger logger = Logger.getLogger(CredentialStore.class);

    @JsonProperty("store")
    private Map<URI, AuthToken> store;

    @JsonCreator
    CredentialStore(@JsonProperty("store") Map<URI, AuthToken> store) {
        this.store = store;
    }

    CredentialStore() {
        this(new HashMap<>());
    }

    private Map<URI, AuthToken> getStore() {
        if (Optional.ofNullable(store).isEmpty()) {
            store = new HashMap<>();
        }
        return store;
    }

    /**
     * Adds or replaces an entry.
     * @param authToken
     */
    public void add(AuthToken authToken) {
        getStore().put(authToken.getRegistry(), authToken);
    }

    public void remove(URI registry) {
        getStore().remove(registry);
    }

    public Optional<AuthToken> get(URI registry) {
        return Optional.ofNullable(getStore().get(registry));
    }
}
