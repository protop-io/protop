package io.protop.core.manifest;

import io.protop.core.error.ServiceException;
import io.protop.core.error.ServiceExceptionConsumer;

public class ManifestNotFound extends ServiceException {

    public ManifestNotFound() {
        super("Did not find a manifest (protop.json).");
    }

    public ManifestNotFound(String message) {
        super(message);
    }

    @Override
    public void accept(ServiceExceptionConsumer consumer) {
        consumer.consume(this);
    }
}
