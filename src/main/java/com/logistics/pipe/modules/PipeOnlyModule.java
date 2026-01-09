package com.logistics.pipe.modules;

public class PipeOnlyModule implements Module {
    @Override
    public boolean allowsInventoryConnections() {
        return false;
    }
}
