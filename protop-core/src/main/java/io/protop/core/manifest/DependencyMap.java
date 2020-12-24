package io.protop.core.manifest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.protop.core.manifest.revision.RevisionSource;
import io.protop.core.manifest.revision.RevisionUtils;
import io.protop.registry.domain.Package;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DependencyMap {

    @JsonValue
    private final Map<PackageId, RevisionSource> values;

    @JsonCreator
    public DependencyMap( Map<PackageId, RevisionSource> values) {
        this.values = values;
    }

    public DependencyMap() {
        this(new HashMap<>());
    }

    public void add(PackageId name, RevisionSource revisionSource) {
        values.put(name, revisionSource);
    }

    public Map<PackageId, RevisionSource> getValues() {
        return new HashMap<>(values);
    }

    public static DependencyMap empty() {
        return new DependencyMap();
    }

    public static DependencyMap from(List<Package.Dependency> dependencyList) {
        Map<PackageId, RevisionSource> map = new HashMap<>();
        dependencyList.forEach(dependency -> {
            map.put(PackageId.from(dependency.getPackageId()),
                    RevisionUtils.fromString(dependency.getSource()));
        });
        return new DependencyMap(map);
    }
}
