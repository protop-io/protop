package io.protop.core.manifest.converters;

import com.fasterxml.jackson.databind.util.StdConverter;
import io.protop.core.manifest.PackageId;
import io.protop.core.manifest.DependencyMap;
import io.protop.core.manifest.revision.RevisionSource;
import io.protop.core.manifest.revision.RevisionUtils;

import java.util.HashMap;
import java.util.Map;

public class DependencyMapDeserializer extends StdConverter<Map<PackageId, String>, DependencyMap> {

    @Override
    public DependencyMap convert(Map<PackageId, String> value) {
        Map<PackageId, RevisionSource> convertedMap = new HashMap<>();

        for (PackageId packageId : value.keySet()) {
            String input = value.get(packageId);
            RevisionSource revisionSource = RevisionUtils.fromString(input);
            convertedMap.put(packageId, revisionSource);
        }

        return new DependencyMap(convertedMap);
    }
}
