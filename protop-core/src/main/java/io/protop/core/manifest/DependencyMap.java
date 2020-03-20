package io.protop.core.manifest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.protop.core.manifest.revision.RevisionSource;

import java.util.HashMap;
import java.util.Map;

public class DependencyMap {

    @JsonValue
    private final Map<Coordinate, RevisionSource> values;

    @JsonCreator
    public DependencyMap( Map<Coordinate, RevisionSource> values) {
        this.values = values;
    }

    public DependencyMap() {
        this(new HashMap<>());
    }

    public void add(Coordinate name, RevisionSource revisionSource) {
        values.put(name, revisionSource);
    }

    public Map<Coordinate, RevisionSource> getValues() {
        return new HashMap<>(values);
    }

    public static DependencyMap empty() {
        return new DependencyMap();
    }
}
