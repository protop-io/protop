package io.protop.core.link;

import io.protop.core.manifest.Coordinate;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.file.Path;

@Getter
@AllArgsConstructor
public class LinkDetail {

    Coordinate coordinate;
    Path path;
}
