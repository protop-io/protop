package io.protop.core.manifest;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import io.protop.core.manifest.converters.PathListToStringList;
import io.protop.core.manifest.converters.StringToVersion;
import io.protop.core.manifest.converters.VersionToString;
import io.protop.core.error.ServiceError;
import io.protop.core.error.ServiceException;
import io.protop.version.Version;
import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"name", "org", "version", "description", "keywords", "license", "homepage"})
public class Manifest {

    private static final Logger logger = LoggerFactory.getLogger(Manifest.class);
    public static final String PROTOP_JSON = "protop.json";

    @JsonProperty("name")
    protected String name;

    @JsonProperty("version")
    @JsonSerialize(converter = VersionToString.class)
    private Version version;

    @JsonProperty("organization")
    private String organization;

    @JsonProperty("include")
    @JsonSerialize(converter = PathListToStringList.class)
    private List<Path> include;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("dependencies")
    private DependencyMap dependencies;

    @JsonProperty("description")
    private String description;

    @JsonProperty("readme")
    private String readme;

    @JsonProperty("keywords")
    private List<String> keywords;

    @JsonProperty("homepage")
    private URI homepage;

    @JsonProperty("license")
    private String license;

    @JsonCreator
    Manifest(
            @JsonProperty("name") @NotNull String name,
            @JsonProperty("version") @JsonDeserialize(converter = StringToVersion.class) @NotNull Version version,
            @JsonProperty("organization") @NotNull String organization,
            @JsonProperty("include") @NotNull List<Path> include,
            @JsonProperty("dependencies") DependencyMap dependencies,
            @JsonProperty("description") String description,
            @JsonProperty("readme") String readme,
            @JsonProperty("keywords") List<String> keywords,
            @JsonProperty("homepage") URI homepage,
            @JsonProperty("license") String license) {
        this.name = name;
        this.version = version;
        this.organization = organization;
        this.include = ImmutableList.copyOf(include);
        this.dependencies = dependencies;
        this.description = description;
        this.readme = readme;
        this.keywords = keywords;
        this.homepage = homepage;
        this.license = license;
    }

    public static Optional<Manifest> from(Path directory) {
        Path configurationPath = directory.resolve(PROTOP_JSON);

        if (!Files.isRegularFile(configurationPath)) {
            return Optional.empty();
        }

        File file = configurationPath.toFile();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            Manifest manifest = objectMapper
                    .readValue(file, Manifest.class);
            return Optional.of(manifest);
        } catch (IOException e) {
            logger.error("Failed to parse configuration.", e);
            throw new ServiceException(ServiceError.MANIFEST_ERROR,
                    "Failed to parse configuration.");
        }
    }
}
