package io.protop.core.grpc;

import io.protop.core.auth.AuthService;
import io.protop.core.auth.AuthToken;
import lombok.AllArgsConstructor;

import java.net.URL;

@AllArgsConstructor
public class GrpcService {

    private final AuthService<?> authService;

    public AuthTokenCallCredentials getAuthCredentials(URL url) {
        AuthToken token = authService.getOrCreateToken(url).blockingGet();
        return new AuthTokenCallCredentials(token.getValue());
    }
}
