package io.protop.core.manifest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableMap;
import io.protop.core.manifest.converters.DependencyMapFromRaw;
import io.protop.version.Version;

import java.util.HashMap;
import java.util.Map;

public class DependencyMap {

    @JsonValue
    private final Map<ProjectCoordinate, Version> values;

    @JsonCreator
    DependencyMap(Map<ProjectCoordinate, String> values) {
        this.values = ImmutableMap.copyOf(new DependencyMapFromRaw().convert(values));
    }

    public DependencyMap() {
        this(new HashMap<>());
    }

    public void add(ProjectCoordinate name, Version version) {
        values.put(name, version);
    }

    public Map<ProjectCoordinate, Version> getValues() {
        return new HashMap<>(values);
    }
}
