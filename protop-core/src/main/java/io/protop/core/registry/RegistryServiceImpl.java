package io.protop.core.registry;

import io.protop.core.Environment;

import java.net.URI;

public class RegistryServiceImpl implements RegistryService {

    /**
     *
     * @return
     */
    @Override
    public URI getDefaultRegistry() {
        return Environment.UNIVERSAL_DEFAULT_REGISTRY;
    }
}
