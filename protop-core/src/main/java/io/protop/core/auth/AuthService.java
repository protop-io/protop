package io.protop.core.auth;

import io.grpc.Channel;
import io.grpc.stub.StreamObserver;
import io.protop.core.Context;
import io.protop.core.RuntimeConfiguration;
import io.protop.core.grpc.AuthTokenCallCredentials;
import io.protop.core.grpc.GrpcService;
import io.protop.core.logs.Logger;
import io.protop.core.storage.Storage;
import io.protop.core.storage.StorageService;
import io.protop.registry.services.Auth;
import io.protop.registry.services.AuthServiceGrpc;
import io.reactivex.Completable;
import io.reactivex.Maybe;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

public class AuthService {

    private static final Logger logger = Logger.getLogger(AuthService.class);

    private static final String CREDENTIAL_STORE_FILE_NAME = ".auth.json";

    private final StorageService storageService;
    private final Path storageFilePath;
    private final GrpcService grpcService;
    private final Context context;

    public AuthService(StorageService storageService, GrpcService grpcService, Context context) {
        this.storageService = storageService;
        this.grpcService = grpcService;
        this.context = context;
        this.storageFilePath = Storage.pathOf(Storage.GlobalDirectory.SESSION_STORE)
                .resolve(CREDENTIAL_STORE_FILE_NAME);
    }

    private UserCredentials getCredentialsFromRc(URL registry) {
        RuntimeConfiguration rc = context.getRc();
        return UserCredentials.builder()
                .registry(registry)
                .username(rc.getUsername())
                .password(rc.getPassword())
                .build();
    }

    public Maybe<StoredUserCredentialsItem> getOrCreateToken(URL registry) {
        return getStoredToken(registry)
                .switchIfEmpty(Maybe.just(StoredUserCredentialsItem.of(getCredentialsFromRc(registry))))
                .map(item -> {
                    if (item.isExpired()) {
                        return authorize(UserCredentials.of(item)).blockingGet(); }
                    else {
                        return item;
                    }
                });
    }

    public AuthTokenCallCredentials getAuthTokenCallCredentials(URL url) {
        StoredUserCredentialsItem item = getOrCreateToken(url)
                .blockingGet();
        return new AuthTokenCallCredentials(item.getToken());
    }

    private AuthServiceGrpc.AuthServiceStub createAuthServiceStub(URL url) {
        Channel channel = grpcService.getChannel(url);
        return AuthServiceGrpc.newStub(channel);
    }

    /**
     * Handles any authorization of the credentials for future work.
     * @param credentials the credentials to authorize.
     * @return completable object.
     */
    public Maybe<StoredUserCredentialsItem> authorize(UserCredentials credentials) {
        return Maybe.create(emitter -> {
            logger.info("Authenticating with auth service.");
            CredentialStore credentialStore = loadCredentialStore();

            AuthServiceGrpc.AuthServiceStub authServiceStub = createAuthServiceStub(credentials.getRegistry());
            authServiceStub.login(Auth.LoginRequest.newBuilder()
                    .setUsername(credentials.getUsername())
                    .setPassword(credentials.getPassword())
                    .build(), new StreamObserver<>() {
                @Override
                public void onNext(Auth.LoginResponse value) {
                    StoredUserCredentialsItem token = StoredUserCredentialsItem.builder()
                            .registry(credentials.getRegistry())
                            .token(value.getToken())
                            .username(credentials.getUsername())
                            .password(credentials.getPassword())
                            .timestamp(System.currentTimeMillis())
                            .ttl(value.getExpiresIn())
                            .build();
                    credentialStore.add(token);
                    save(credentialStore);
                    logger.info("New auth token granted and saved.");
                    emitter.onSuccess(token);
                }

                @Override
                public void onError(Throwable t) {
                    emitter.onError(new AuthenticationFailed("Failed to authenticate credentials in the registry."));
                }

                @Override
                public void onCompleted() {
                    emitter.onComplete();
                }
            });
        });
    }

    public Maybe<StoredUserCredentialsItem> getStoredToken(URL registry) {
        return Maybe.fromCallable(() -> {
            CredentialStore credentialStore = loadCredentialStore();
            return credentialStore.get(registry).orElse(null);
        });
    }

    /**
     * Forget/discard credentials for registry.
     * @param registry the registry to forget.
     * @return completable object.
     */
    public Completable forget(URL registry) {
        return Completable.create(emitter -> {
            CredentialStore credentialStore = loadCredentialStore();
            credentialStore.remove(registry);
            save(credentialStore);
            emitter.onComplete();
        });
    }

    private void save(CredentialStore credentialStore) {
        storageService.storeJson(credentialStore, storageFilePath);
    }

    private CredentialStore loadCredentialStore() {
        return storageService.loadResource(storageFilePath, CredentialStore.class)
                .blockingGet(new CredentialStore());
    }

    private URL createLoginUrl(UserCredentials credentials) throws MalformedURLException {
        return credentials.getRegistry();
    }
}
