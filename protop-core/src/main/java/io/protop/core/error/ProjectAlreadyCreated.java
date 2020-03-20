package io.protop.core.error;

public class ProjectAlreadyCreated extends ServiceException {

    public ProjectAlreadyCreated() {
        super("A project was already created in the target directory.");
    }

    @Override
    public void accept(ServiceExceptionConsumer consumer) {
        consumer.consume(this);
    }
}
