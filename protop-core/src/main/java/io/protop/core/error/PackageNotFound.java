package io.protop.core.error;

import io.protop.core.manifest.Coordinate;
import io.protop.core.manifest.revision.RevisionSource;

public final class PackageNotFound extends ServiceException {

    public PackageNotFound(Coordinate coordinate, RevisionSource revisionSource) {
        super(String.format(
                "Could not find package %s %s.",
                coordinate,
                revisionSource));
    }

    @Override
    public void accept(ServiceExceptionConsumer consumer) {
        consumer.consume(this);
    }
}
