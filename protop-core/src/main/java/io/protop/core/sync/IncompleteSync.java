package io.protop.core.sync;

import io.protop.calver.CalVer;
import io.protop.core.config.ProjectId;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IncompleteSync extends Exception {

    private final Map<ProjectId, CalVer> unresolvedDependencies;
}
