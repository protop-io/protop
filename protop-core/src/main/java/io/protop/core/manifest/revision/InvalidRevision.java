package io.protop.core.manifest.revision;

import io.protop.core.error.ServiceException;
import io.protop.core.error.ServiceExceptionConsumer;

public class InvalidRevision extends ServiceException {

    public InvalidRevision(String message) {
        super(message);
    }

    @Override
    public void accept(ServiceExceptionConsumer consumer) {
        consumer.consume(this);
    }
}
