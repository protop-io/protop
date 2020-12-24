package io.protop.core.error;

import io.protop.core.manifest.PackageId;
import io.protop.core.manifest.revision.RevisionSource;

public final class PackageNotFound extends ServiceException {

    public PackageNotFound(PackageId packageId, RevisionSource revisionSource) {
        super(String.format(
                "Could not find package %s %s.",
                packageId,
                revisionSource));
    }

    @Override
    public void accept(ServiceExceptionConsumer consumer) {
        consumer.consume(this);
    }
}
