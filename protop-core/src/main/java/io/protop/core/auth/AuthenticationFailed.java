package io.protop.core.auth;

import io.protop.core.error.ServiceException;
import io.protop.core.error.ServiceExceptionConsumer;

public class AuthenticationFailed extends ServiceException {

    public AuthenticationFailed(String message) {
        super(message);
    }

    @Override
    public void accept(ServiceExceptionConsumer consumer) {
        consumer.consume(this);
    }
}
