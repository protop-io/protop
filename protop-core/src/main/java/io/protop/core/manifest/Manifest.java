package io.protop.core.manifest;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.protop.core.Environment;
import io.protop.core.logs.Logger;
import io.protop.core.manifest.converters.DependencyMapDeserializer;
import io.protop.core.manifest.revision.Version;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Getter
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"name", "org", "version", "description", "keywords", "license", "homepage"})
public class Manifest {

    private static final Logger logger = Logger.getLogger(Manifest.class);
    public static final String PROTOP_JSON = "protop.json";

    @JsonProperty("name")
    protected String name;

    @JsonProperty("version")
    private Version version;

    @JsonProperty("organization")
    private String organization;

//    @JsonProperty("include")
//    @JsonSerialize(converter = PathListToStringList.class)
//    private List<Path> include;

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
            @JsonProperty("version") @NotNull Version version,
            @JsonProperty("organization") @NotNull String organization,
//            @JsonProperty("include") List<Path> include,
            @JsonProperty("dependencies") @JsonDeserialize(converter = DependencyMapDeserializer.class)
                    DependencyMap dependencies,
            @JsonProperty("description") String description,
            @JsonProperty("readme") String readme,
            @JsonProperty("keywords") List<String> keywords,
            @JsonProperty("homepage") URI homepage,
            @JsonProperty("license") String license) {
        this.name = name;
        this.version = version;
        this.organization = organization;
//        this.include = Objects.isNull(include) ? ImmutableList.of() : ImmutableList.copyOf(include);
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
        ObjectMapper objectMapper = Environment.getInstance().getObjectMapper();

        try {
            Manifest manifest = objectMapper
                    .readValue(file, Manifest.class);
            return Optional.of(manifest);
        } catch (IOException e) {
            String message = "Failed to parse configuration.";
            logger.error(message, e);
            throw new RuntimeException(message, e);
        }
    }
}
