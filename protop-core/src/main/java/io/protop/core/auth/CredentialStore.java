package io.protop.core.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import io.protop.core.logs.Logger;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import lombok.Builder;
import lombok.Getter;

/**
 * Contains all saved credentials mapping to registries. Multiple credentials are not allowed for a single registry.
 */
@Getter
@Builder
public class CredentialStore {

    private static final Logger logger = Logger.getLogger(CredentialStore.class);

    @JsonProperty("store")
    private Map<URI, BasicCredentials> store;

    @JsonCreator
    CredentialStore(@JsonProperty("store") Map<URI, BasicCredentials> store) {
        this.store = store;
    }

    CredentialStore() {
        this(new HashMap<>());
    }

    private Map<URI, BasicCredentials> getStore() {
        if (Optional.ofNullable(store).isEmpty()) {
            store = new HashMap<>();
        }
        return store;
    }

    /**
     * Adds or replaces an entry.
     * @param basicCredentials
     */
    public void add(BasicCredentials basicCredentials) {
        getStore().put(basicCredentials.getRegistry(), basicCredentials);
    }

    public void remove(URI registry) {
        getStore().remove(registry);
    }

    public BasicCredentials get(URI registry) {
        return getStore().get(registry);
    }
}
