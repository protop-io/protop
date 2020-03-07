package io.protop.core.sync;

import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor
public class DependencyResolutionConfiguration {

    final boolean includesLinkedDependencies;
}
