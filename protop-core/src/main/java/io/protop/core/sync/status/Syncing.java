package io.protop.core.sync.status;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Syncing implements SyncStatus {

    private final String description;

    @Override
    public String getMessage() {
        return String.format("Syncing %s.", description);
    }
}
