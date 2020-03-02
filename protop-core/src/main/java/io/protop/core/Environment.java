package io.protop.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.net.URI;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class Environment {

    public static URI UNIVERSAL_DEFAULT_REGISTRY = URI.create(
            "https://registry.protop.io");

    private static Environment instance;

    private final ObjectMapper objectMapper;

    public static Environment getInstance() {
        if (instance == null) {
            instance = new Environment(createObjectMapper());
        }
        return instance;
    }

    public String getVersion() {
        return "0.1.0";
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        return objectMapper;
    }
}
