package io.protop.core.sync;

import io.protop.core.error.ServiceException;
import io.protop.core.error.ServiceExceptionConsumer;
import io.protop.core.manifest.Coordinate;
import io.protop.core.manifest.revision.RevisionSource;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class IncompleteSync extends ServiceException {

    private final Map<Coordinate, RevisionSource> unresolvedDependencies;

    public IncompleteSync() {
        this(new HashMap<>());
    }

    public IncompleteSync(Map<Coordinate, RevisionSource> unresolvedDependencies) {
        super("Failed to resolve dependencies.");
        this.unresolvedDependencies = unresolvedDependencies;
    }

    public void accept(ServiceExceptionConsumer consumer) {
        consumer.consume(this);
    }
}
