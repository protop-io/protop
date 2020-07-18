package io.protop.core.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.protop.api.auth.AuthTokenResponse;
import io.protop.core.Environment;
import io.protop.core.logs.Logger;
import io.protop.core.storage.Storage;
import io.protop.core.storage.StorageService;
import io.protop.utils.HttpUtils;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

public class BasicAuthService implements AuthService<BasicCredentials> {

    private static final Logger logger = Logger.getLogger(BasicAuthService.class);

    private static final String CREDENTIAL_STORE_FILE_NAME = ".auth.json";

    private final StorageService storageService;
    private final Path storageFilePath;

    public BasicAuthService(StorageService storageService) {
        this.storageService = storageService;
        this.storageFilePath = Storage.pathOf(Storage.GlobalDirectory.SESSION_STORE)
                .resolve(CREDENTIAL_STORE_FILE_NAME);
    }

    @Override
    public Maybe<AuthToken> getOrCreateToken(URL registry) {
        return getStoredToken(registry)
                .switchIfEmpty(Maybe.just(new AuthToken(registry, "TODO")));
    }

    @Override
    public Completable authorize(BasicCredentials credentials) {
        return Completable.create(emitter -> {
            CredentialStore credentialStore = loadCredentialStore();

            ObjectMapper objectMapper = Environment.getInstance().getObjectMapper();

            HttpClient client = HttpUtils.createHttpClient();

            String body = objectMapper.writeValueAsString(credentials);
            HttpPut put = new HttpPut(createLoginUri(credentials).toURI());
            put.setEntity(new StringEntity(body));

            HttpResponse response = client.execute(put);
            String entity = EntityUtils.toString(response.getEntity());

            int status = response.getStatusLine().getStatusCode();
            if (status == HttpStatus.SC_OK || status == HttpStatus.SC_CREATED) {
                AuthTokenResponse authTokenResponse = objectMapper.readValue(
                        entity, AuthTokenResponse.class);
                credentialStore.add(AuthToken.builder()
                        .registry(credentials.getRegistry())
                        .value(authTokenResponse.getToken())
                        .build());
                save(credentialStore);
                emitter.onComplete();
            } else {
                // TODO range of responses handle better
                logger.info("Not OK status: " + status);
                logger.info("Not OK response: " + entity);

                emitter.onError(new AuthenticationFailed("Unhappy response from the registry."));
            }
        });
    }

    @Override
    public Maybe<AuthToken> getStoredToken(URL registry) {
        return Maybe.fromCallable(() -> {
            CredentialStore credentialStore = loadCredentialStore();
            return credentialStore.get(registry).orElse(null);
        });
    }

    @Override
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

    private URL createLoginUri(BasicCredentials credentials) throws MalformedURLException {
        return credentials.getRegistry();
//                .setPath(credentials.getRegistry().getPath()
//                        +  "/TODO/TODO:" + credentials.getUsername())
//                .build();
    }
}
