package io.protop.core.publish;

import io.protop.core.error.ServiceException;
import io.protop.core.error.ServiceExceptionConsumer;

public class PublishFailed extends ServiceException {

    public PublishFailed(String message) {
        super(message);
    }

    @Override
    public void accept(ServiceExceptionConsumer consumer) {
        consumer.consume(this);
    }
}
