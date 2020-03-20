package io.protop.core.manifest.converters;

import com.fasterxml.jackson.databind.util.StdConverter;
import io.protop.core.manifest.Coordinate;
import io.protop.core.manifest.DependencyMap;
import io.protop.core.manifest.revision.RevisionSource;
import io.protop.core.manifest.revision.RevisionUtils;

import java.util.HashMap;
import java.util.Map;

public class DependencyMapDeserializer extends StdConverter<Map<Coordinate, String>, DependencyMap> {

    @Override
    public DependencyMap convert(Map<Coordinate, String> value) {
        Map<Coordinate, RevisionSource> convertedMap = new HashMap<>();

        for (Coordinate coordinate : value.keySet()) {
            String input = value.get(coordinate);
            RevisionSource revisionSource = RevisionUtils.fromString(input);
            convertedMap.put(coordinate, revisionSource);
        }

        return new DependencyMap(convertedMap);
    }
}
