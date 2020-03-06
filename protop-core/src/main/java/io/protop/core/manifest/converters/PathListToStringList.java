package io.protop.core.manifest.converters;

import com.fasterxml.jackson.databind.util.StdConverter;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class PathListToStringList extends StdConverter<List<Path>, List<String>> {

    @Override
    public List<String> convert(List<Path> values) {
        return values.stream()
                .map(Path::toString)
                .collect(Collectors.toList());
    }
}
