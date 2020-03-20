package io.protop.core.storage;

import io.protop.core.error.ServiceException;
import io.protop.core.error.ServiceExceptionConsumer;

public class StorageException extends ServiceException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable caught) {
        super(message, caught);
    }

    @Override
    public void accept(ServiceExceptionConsumer consumer) {
        consumer.consume(this);
    }
}
