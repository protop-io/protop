package io.protop.core.auth;

import io.reactivex.Completable;
import io.reactivex.Maybe;

import java.net.URL;

public interface AuthService<T extends Credentials> {

    /**
     * Handles any authorization of the credentials for future work.
     * @param credentials the credentials to authorize.
     * @return completable object.
     */
    public Completable authorize(T credentials);

    /**
     * Forget/discard credentials for registry.
     * @param registry the registry to forget.
     * @return completable object.
     */
    public Completable forget(URL registry);

    public Maybe<AuthToken> getStoredToken(URL registry);

    public Maybe<AuthToken> getOrCreateToken(URL registry);
}
