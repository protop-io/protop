package io.protop.core.grpc;

import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;

import java.util.concurrent.Executor;

public class AuthTokenCallCredentials extends CallCredentials {

    public static final Metadata.Key<String> AUTHORIZATION_METADATA_KEY = Metadata.Key.of(
            "auth_token", Metadata.ASCII_STRING_MARSHALLER);

    private final String token;

    public AuthTokenCallCredentials(String token) {
        this.token = token;
    }

    @Override
    public void applyRequestMetadata(RequestInfo requestInfo, Executor appExecutor, MetadataApplier applier) {
        appExecutor.execute(() -> {
            try {
                Metadata headers = new Metadata();
                headers.put(AUTHORIZATION_METADATA_KEY, token);
                applier.apply(headers);
            } catch (Throwable t) {
                applier.fail(Status.UNAUTHENTICATED.withCause(t));
            }
        });
    }

    @Override
    public void thisUsesUnstableApi() {
        // no op
    }
}
