package io.protop.core.grpc;

import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import lombok.AllArgsConstructor;

import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@AllArgsConstructor
public class GrpcService {

    private final ConcurrentMap<URL, Channel> channels;


    public GrpcService() {
        this.channels = new ConcurrentHashMap<>();
    }

    public Channel getChannel(URL url) {
        return channels.computeIfAbsent(url, v -> ManagedChannelBuilder
                .forAddress(url.getHost(), url.getPort())
                .usePlaintext()
                .build());
    }
}
