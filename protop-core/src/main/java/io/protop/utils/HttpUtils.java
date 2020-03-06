package io.protop.utils;

import com.google.common.collect.ImmutableList;
import io.protop.core.auth.AuthToken;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;

public class HttpUtils {

    public static HttpClient createHttpClient() {
        return HttpClientBuilder.create()
                .build();
    }

    public static HttpClient createHttpClientWithToken(AuthToken token) {
        return HttpClientBuilder.create()
                .setDefaultHeaders(ImmutableList.of(new BasicHeader(
                        "Authorization", "Bearer " + token.getValue())))
                .build();
    }
}
