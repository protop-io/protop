package io.protop.core.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableMap;
import io.protop.calver.CalVer;
import io.protop.core.config.converters.DependencyMapFromRaw;

import java.util.HashMap;
import java.util.Map;

public class DependencyMap {

    @JsonValue
    private final Map<ProjectId, CalVer> values;

    @JsonCreator
    DependencyMap(Map<ProjectId, String> values) {
        this.values = ImmutableMap.copyOf(new DependencyMapFromRaw().convert(values));
    }

    public DependencyMap() {
        this(new HashMap<>());
    }

    public void add(ProjectId name, CalVer version) {
        values.put(name, version);
    }

    public Map<ProjectId, CalVer> getValues() {
        return new HashMap<>(values);
    }
}
