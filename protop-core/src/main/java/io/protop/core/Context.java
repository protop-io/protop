package io.protop.core;

import io.protop.core.logs.Logger;
import io.protop.core.manifest.Manifest;
import io.protop.core.manifest.ManifestNotFound;
import io.protop.core.storage.Storage;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Provides details about the execution context.
 */
@Getter
@Immutable
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Context {

    private static final Logger logger = Logger.getLogger(Context.class);

    private static final RuntimeConfiguration defaultRc = RuntimeConfiguration.builder()
            .repositoryUri(Environment.UNIVERSAL_DEFAULT_REGISTRY)
            .build();

    @NotNull
    private final Path projectLocation;

    @NotNull
    private final Manifest manifest;

    @NotNull
    private final RuntimeConfiguration rc;

    public static Context from(@NotNull Path projectLocation, RuntimeConfiguration... rcs) {
        Manifest manifest = Manifest.from(projectLocation)
                .orElseThrow(() -> new ManifestNotFound());

        List<RuntimeConfiguration> allRcs = new ArrayList<>(Arrays.asList(rcs));
        allRcs.add(RuntimeConfiguration.from(projectLocation)
                .orElseGet(RuntimeConfiguration::empty));
        allRcs.add(RuntimeConfiguration.from(Storage.getHomePath())
                .orElseGet(RuntimeConfiguration::empty));
        allRcs.add(defaultRc);

        return new Context(projectLocation, manifest, resolveRcs(allRcs));
    }

    private static RuntimeConfiguration resolveRcs(List<RuntimeConfiguration> rcs) {
        AtomicReference<RuntimeConfiguration> resolved = new AtomicReference<>();
        rcs.forEach(rc -> resolved.set(
                Optional.ofNullable(resolved.get())
                        .map(atomicRc -> atomicRc.mergeOver(rc))
                        .orElse(rc)));
        return resolved.get();
    }
}
