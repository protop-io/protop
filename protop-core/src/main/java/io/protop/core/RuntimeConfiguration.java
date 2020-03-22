package io.protop.core;

import com.google.common.base.Strings;
import io.protop.core.logs.Logger;
import io.protop.utils.UriUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
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

@Getter
@Builder
@AllArgsConstructor
public class RuntimeConfiguration {

    private static final Logger logger = Logger.getLogger(RuntimeConfiguration.class);

    private static final String PROTOP_RC = ".protoprc";

    @Nullable
    private final URI repositoryUri;

    @Nullable
    private final URI publishRepositoryUri;

    @Nullable
    private final URI syncRepositoryUri;

    @Nullable
    private final Boolean refreshGitSources;

    public static RuntimeConfiguration empty() {
        return new RuntimeConfiguration(
                null,
                null,
                null,
                null);
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
            String message = "Failed to parse .protoprc configuration.";
            logger.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    private static RuntimeConfiguration runtimeConfigurationFromProperties(Properties props) {
        return RuntimeConfiguration.builder()
                .repositoryUri(UriUtils.fromString(props.getProperty("registry")))
                .publishRepositoryUri(UriUtils.fromString(props.getProperty("publish.registry")))
                .syncRepositoryUri(UriUtils.fromString(props.getProperty("sync.registry")))
                .refreshGitSources(Boolean.valueOf(props.getProperty("git.refresh")))
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
                .refreshGitSources(resolveAsap(getRefreshGitSources(), other.getRefreshGitSources()))
                .build();
    }

    private <T> T resolveAsap(T a, T b) {
        if (a instanceof String) {
            return !Strings.isNullOrEmpty((String) a) ? a : b;
        } else {
            return Objects.nonNull(a) ? a : b;
        }
    }
}
