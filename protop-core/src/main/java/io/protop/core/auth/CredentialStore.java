package io.protop.core.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.protop.core.logs.Logger;
import lombok.Builder;
import lombok.Getter;

import java.net.URL;
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
    private Map<URL, StoredUserCredentialsItem> store;

    @JsonCreator
    CredentialStore(@JsonProperty("store") Map<URL, StoredUserCredentialsItem> store) {
        this.store = store;
    }

    CredentialStore() {
        this(new HashMap<>());
    }

    private Map<URL, StoredUserCredentialsItem> getStore() {
        if (Optional.ofNullable(store).isEmpty()) {
            store = new HashMap<>();
        }
        return store;
    }

    /**
     * Adds or replaces an entry.
     * @param storedUserCredentialsItem
     */
    public void add(StoredUserCredentialsItem storedUserCredentialsItem) {
        getStore().put(storedUserCredentialsItem.getRegistry(), storedUserCredentialsItem);
    }

    public void remove(URL registry) {
        getStore().remove(registry);
    }

    public Optional<StoredUserCredentialsItem> get(URL registry) {
        return Optional.ofNullable(getStore().get(registry));
    }
}
