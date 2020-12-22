package io.protop.core.link;

import io.protop.core.manifest.PackageId;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.file.Path;

@Getter
@AllArgsConstructor
public class LinkDetail {

    PackageId packageId;
    Path path;
}
