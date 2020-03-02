package io.protop.core.auth;

import io.protop.core.storage.Storage;
import io.protop.core.storage.StorageService;
import io.reactivex.Completable;
import io.reactivex.Maybe;

import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;

public class BasicCredentialService implements CredentialService<BasicCredentials> {

    private static final String CREDENTIAL_STORE_FILE_NAME = ".auth.json";

    private final StorageService storageService;
    private final Path storageFilePath;

    public BasicCredentialService(StorageService storageService) {
        this.storageService = storageService;
        this.storageFilePath = Storage.pathOf(Storage.GlobalDirectory.SESSION_STORE)
                .resolve(CREDENTIAL_STORE_FILE_NAME);
    }

    @Override
    public Completable use(BasicCredentials basicCredentials) {
        return Completable.fromCallable(() -> {
            CredentialStore credentialStore = loadCredentialStore();
            credentialStore.add(basicCredentials);
            storageService.storeJson(credentialStore, storageFilePath);
            return null;
        });
    }

    @Override
    public Maybe<BasicCredentials> getStoredCredentials(URI registry) {
        return Maybe.fromCallable(() -> {
            CredentialStore credentialStore = loadCredentialStore();
            return credentialStore.get(registry);
        });
    }

    @Override
    public Completable forget(URI registry) {
        // TODO
        return Completable.complete();
    }

    private CredentialStore loadCredentialStore() {
        return storageService.loadResource(storageFilePath, CredentialStore.class)
                .blockingGet(new CredentialStore());
    }
}
