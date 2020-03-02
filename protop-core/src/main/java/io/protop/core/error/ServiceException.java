package io.protop.core.error;

import java.util.Optional;

public class ServiceException extends RuntimeException {

    private ServiceError error;

    public ServiceException(ServiceError error) {
        this.error = error;
    }

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(ServiceError error, String message) {
        super(message);
        this.error = error;
    }

    public ServiceException(ServiceError error, Throwable caught) {
        super(caught);
        this.error = error;
    }

    public ServiceException(String message, Throwable caught) {
        super(message, caught);
    }

    public Optional<ServiceError> getError() {
        return Optional.ofNullable(error);
    }
}
