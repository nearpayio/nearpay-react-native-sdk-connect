package io.nearpay.reactnative.plugin.operations;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.nearpay.reactnative.plugin.PluginProvider;

public class ProxyShowConnectOperation extends BaseOperation{
    public ProxyShowConnectOperation(PluginProvider provider) {
        super(provider);
    }

    @Override
    public void run(Map args, CompletableFuture<Map> promise) {
        provider.getNearpayLib().nearpayProxy.showConnection();
    }

}
