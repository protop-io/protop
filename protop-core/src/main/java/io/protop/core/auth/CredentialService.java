package io.protop.core.auth;

import io.reactivex.Completable;
import io.reactivex.Maybe;

import java.net.URI;

public interface CredentialService<T extends Credentials> {

    /**
     * Handles any exchange of the credentials and/or stores for future use.
     * @param credentials the credentials to use.
     * @return completable object.
     */
    public Completable use(T credentials);

    /**
     * Forget/discard credentials for registry.
     * @param registry the registry to forget.
     * @return completable object.
     */
    public Completable forget(URI registry);

    public Maybe<T> getStoredCredentials(URI registry);
}
