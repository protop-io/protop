package io.protop.core.config;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import io.protop.calver.CalVer;
import io.protop.core.config.converters.PathListToStringList;
import io.protop.core.config.converters.StringToVersion;
import io.protop.core.config.converters.VersionToString;
import io.protop.core.error.ServiceError;
import io.protop.core.error.ServiceException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Configuration {

    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);
    public static final String FILE_NAME = "protop.json";

    @JsonProperty("name")
    private String name;

    @JsonProperty("version")
    @JsonSerialize(converter = VersionToString.class)
    private CalVer version;

    @JsonProperty("organization")
    private String organization;

    @JsonProperty("include")
    @JsonSerialize(converter = PathListToStringList.class)
    private List<Path> include;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("dependencies")
    private DependencyMap dependencies;

    @JsonCreator
    Configuration(
            @JsonProperty("name") @NotNull String name,
            @JsonProperty("version") @JsonDeserialize(converter = StringToVersion.class) @NotNull CalVer version,
            @JsonProperty("organization") @NotNull String organization,
            @JsonProperty("include") @NotNull List<Path> include,
            @JsonProperty("dependencies") DependencyMap dependencies) {
        this.name = name;
        this.version = version;
        this.organization = organization;
        this.include = ImmutableList.copyOf(include);
        this.dependencies = dependencies;
    }

    public static Optional<Configuration> from(Path directory) {
        Path configurationPath = directory.resolve(FILE_NAME);

        if (!Files.isRegularFile(configurationPath)) {
            return Optional.empty();
        }

        File file = configurationPath.toFile();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            Configuration configuration = objectMapper
                    .readValue(file, Configuration.class);
            return Optional.of(configuration);
        } catch (IOException e) {
            logger.error("Failed to parse configuration.", e);
            throw new ServiceException(ServiceError.CONFIGURATION_ERROR,
                    "Failed to parse configuration.");
        }
    }
}
