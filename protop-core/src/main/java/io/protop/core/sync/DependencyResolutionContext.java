package io.protop.core.sync;

import java.nio.file.Path;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor
public class DependencyResolutionContext {

    final Path projectPath;

    final boolean includesLinkedDependencies;
}
