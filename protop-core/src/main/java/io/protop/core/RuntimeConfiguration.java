package io.protop.core;

import com.google.common.base.Strings;
import io.protop.core.logs.Logger;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
    private final String repositoryUrl;

    @Nullable
    private final String publishRepositoryUrl;

    @Nullable
    private final String syncRepositoryUrl;

    @Nullable
    private final Boolean refreshGitSources;

    @Nullable
    private final String username;

    @Nullable
    private final String password;

    public static RuntimeConfiguration empty() {
        return new RuntimeConfiguration(
                null,
                null,
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
                .repositoryUrl(props.getProperty("registry"))
                .publishRepositoryUrl(props.getProperty("publish.registry"))
                .syncRepositoryUrl(props.getProperty("sync.registry"))
                .refreshGitSources(Boolean.valueOf(props.getProperty("git.refresh")))
                .username(props.getProperty("username"))
                .password(props.getProperty("password"))
                .build();
    }

    public RuntimeConfiguration mergeOver(@NotNull RuntimeConfiguration other) {
        if (Objects.isNull(other)) {
            throw new InvalidParameterException("Cannot be null");
        }

        return RuntimeConfiguration.builder()
                .repositoryUrl(resolveAsap(getRepositoryUrl(), other.getRepositoryUrl()))
                .publishRepositoryUrl(resolveAsap(getPublishRepositoryUrl(), other.getPublishRepositoryUrl()))
                .syncRepositoryUrl(resolveAsap(getSyncRepositoryUrl(), other.getSyncRepositoryUrl()))
                .refreshGitSources(resolveAsap(getRefreshGitSources(), other.getRefreshGitSources()))
                .username(resolveAsap(getUsername(), other.getUsername()))
                .password(resolveAsap(getPassword(), other.getPassword()))
                .build();
    }

    private <T> T resolveAsap(T a, T b) {
        if (a instanceof String) {
            return !Strings.isNullOrEmpty((String) a) ? a : b;
        } else {
            return Objects.nonNull(a) ? a : b;
        }
    }

    @Nullable
    public String getPublishRepositoryUrl() {
        return Optional.ofNullable(publishRepositoryUrl).orElse(repositoryUrl);
    }

    @Nullable
    public String getSyncRepositoryUrl() {
        return Optional.ofNullable(syncRepositoryUrl).orElse(repositoryUrl);
    }
}
