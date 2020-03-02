package io.protop.core.config;

import io.protop.core.error.ServiceException;

public class InvalidDependencyName extends ServiceException {

    InvalidDependencyName(String name) {
        super(String.format("Invalid dependency name: %s.", name));
    }
}
