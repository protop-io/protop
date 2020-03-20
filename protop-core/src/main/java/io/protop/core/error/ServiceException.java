package io.protop.core.error;

/**
 * A runtime exception that will stop the process from continuing.
 */
public abstract class ServiceException extends RuntimeException {

    protected ServiceException(String message) {
        super(message);
    }

    protected ServiceException(Throwable caught) {
        super(caught);
    }

    protected ServiceException(String message, Throwable caught) {
        super(message, caught);
    }

    public abstract void accept(ServiceExceptionConsumer consumer);
}
