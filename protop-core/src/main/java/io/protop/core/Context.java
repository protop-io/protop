package io.protop.core;

import io.protop.core.error.ServiceError;
import io.protop.core.error.ServiceException;
import io.protop.core.manifest.Manifest;
import io.protop.core.storage.Storage;
import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Provides details about the execution context.
 */
@Getter
@Immutable
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Context {

    private static final RuntimeConfiguration defaultRc = RuntimeConfiguration.builder()
            .repositoryUri(Environment.UNIVERSAL_DEFAULT_REGISTRY)
            .build();

    @NotNull
    private final Manifest manifest;

    @NotNull
    private final RuntimeConfiguration rc;

    public static Context from(Path projectLocation, RuntimeConfiguration... rcs) {
        Manifest manifest = Manifest.from(projectLocation)
                .orElseThrow(() -> new ServiceException(ServiceError.MANIFEST_ERROR, "Manifest not found."));

        List<RuntimeConfiguration> allRcs = new ArrayList<>(Arrays.asList(rcs));
        allRcs.add(RuntimeConfiguration.from(projectLocation)
                .orElseGet(RuntimeConfiguration::empty));
        allRcs.add(RuntimeConfiguration.from(Storage.getHomePath())
                .orElseGet(RuntimeConfiguration::empty));

        return new Context(manifest, resolveRcs(allRcs));
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
