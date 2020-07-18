package io.protop.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Strings;
import io.protop.core.logs.Logger;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Optional;
import java.util.jar.Manifest;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class Environment {

    private static final Logger logger = Logger.getLogger(Environment.class);

    public static String UNIVERSAL_DEFAULT_REGISTRY = "https://registry.protop.io";

    private static final String JAR_MANIFEST_NAME = "META-INF/MANIFEST.MF";
    private static final String PROTOP_VERSION_NAME = "Protop-Version";
    private static Environment instance;

    private final ObjectMapper objectMapper;

    public static Environment getInstance() {
        if (instance == null) {
            instance = new Environment(createObjectMapper());
        }
        return instance;
    }

    public Optional<String> getVersion() {
        try {
            Enumeration<URL> resources = getClass().getClassLoader().getResources(JAR_MANIFEST_NAME);
            while (resources.hasMoreElements()) {
                Manifest manifest = new Manifest(resources.nextElement().openStream());
                String version = manifest.getMainAttributes().getValue(PROTOP_VERSION_NAME);
                if (!Strings.isNullOrEmpty(version)) {
                    return Optional.of(version);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to read from Jar manifest.", e);
        }
        return Optional.empty();
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        return objectMapper;
    }
}
