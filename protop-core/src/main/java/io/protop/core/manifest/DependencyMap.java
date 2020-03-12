package io.protop.core.manifest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.protop.core.version.Version;

import java.util.HashMap;
import java.util.Map;

public class DependencyMap {

    @JsonValue
    private final Map<ProjectCoordinate, Version> values;

    @JsonCreator
    DependencyMap(Map<ProjectCoordinate, Version> values) {
        this.values = values;
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
