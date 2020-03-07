package io.protop.core;

import io.protop.core.error.ServiceError;
import io.protop.core.error.ServiceException;
import io.protop.core.logs.Logger;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import javax.validation.constraints.NotNull;
import io.protop.utils.UriUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RuntimeConfiguration {

    private static final Logger logger = Logger.getLogger(RuntimeConfiguration.class);

    private static final String PROTOP_RC = ".protoprc";

    private final URI repositoryUri;
    private final URI publishRepositoryUri;
    private final URI syncRepositoryUri;

    public static RuntimeConfiguration empty() {
        return new RuntimeConfiguration(null, null, null);
    }

    public static Optional<RuntimeConfiguration> from(Path directory) {
        Path configurationPath = directory.resolve(PROTOP_RC);
        Properties properties = new Properties();

        if (!Files.isRegularFile(configurationPath)) {
            return Optional.empty();
        }

        File file = configurationPath.toFile();

        try {
            InputStream is = new FileInputStream(file);
            properties.load(is);
            return Optional.of(runtimeConfigurationFromProperties(properties));
        } catch (IOException e) {
            logger.error("Failed to parse configuration.", e);
            throw new ServiceException(ServiceError.RC_ERROR,
                    "Failed to parse .protoprc.");
        }
    }

    private static RuntimeConfiguration runtimeConfigurationFromProperties(Properties props) {
        return RuntimeConfiguration.builder()
                .repositoryUri(UriUtils.fromString(props.getProperty("registry")))
                .publishRepositoryUri(UriUtils.fromString(props.getProperty("publish.registry")))
                .syncRepositoryUri(UriUtils.fromString(props.getProperty("sync.registry")))
                .build();
    }

    public RuntimeConfiguration mergeOver(@NotNull RuntimeConfiguration other) {
        if (Objects.isNull(other)) {
            throw new InvalidParameterException("Cannot be null");
        }

        return RuntimeConfiguration.builder()
                .repositoryUri(resolveAsap(getRepositoryUri(), other.getRepositoryUri()))
                .publishRepositoryUri(resolveAsap(getPublishRepositoryUri(), other.getPublishRepositoryUri()))
                .syncRepositoryUri(resolveAsap(getSyncRepositoryUri(), other.getSyncRepositoryUri()))
                .build();
    }

    private <T> T resolveAsap(T a, T b) {
        return Objects.nonNull(a) ? a : b;
    }
}
