package io.protop.core;

import io.protop.core.error.ServiceError;
import io.protop.core.error.ServiceException;
import io.protop.core.logs.Logger;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.util.Objects;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.ini4j.Ini;

@Getter
@Builder
@AllArgsConstructor
public class RuntimeConfiguration {

    private static final Logger logger = Logger.getLogger(RuntimeConfiguration.class);

    private static final String PROTOP_RC = ".protoprc";

    private final URI repositoryUri;

    public static RuntimeConfiguration empty() {
        return new RuntimeConfiguration(null);
    }

    public static Optional<RuntimeConfiguration> from(Path directory) {
        Path configurationPath = directory.resolve(PROTOP_RC);

        if (!Files.isRegularFile(configurationPath)) {
            return Optional.empty();
        }

        File file = configurationPath.toFile();

        try {
            Ini ini = new Ini(file);
            logger.always("global section name: " + ini.getConfig().getGlobalSectionName());

            return Optional.of(RuntimeConfiguration.builder()
                    .build());
        } catch (IOException e) {
            logger.error("Failed to parse configuration.", e);
            throw new ServiceException(ServiceError.RC_ERROR,
                    "Failed to parse .protoprc.");
        }
    }

    public RuntimeConfiguration mergeOver(@NotNull RuntimeConfiguration other) {
        if (Objects.isNull(other)) {
            throw new InvalidParameterException("Cannot be null");
        }

        return RuntimeConfiguration.builder()
                .repositoryUri(resolveAsap(getRepositoryUri(), other.getRepositoryUri()))
                .build();
    }

    private <T> T resolveAsap(T a, T b) {
        return Objects.nonNull(a) ? a : b;
    }
}
