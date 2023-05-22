package io.nearpay.reactnative.plugin.operations;

import android.annotation.SuppressLint;
import android.app.Application;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.nearpay.proxy.NearpayProxy;
import io.nearpay.reactnative.plugin.ErrorStatus;
import io.nearpay.reactnative.plugin.NearpayLib;
import io.nearpay.reactnative.plugin.PluginProvider;
import io.nearpay.sdk.Environments;
import io.nearpay.sdk.NearPay;

public class InitializeOperation extends BaseOperation {

    public InitializeOperation(PluginProvider provider) {
        super(provider);
    }

    public Map doInitialization(Map args) {
        String authvalue = args.get("authvalue") == null ? "" : args.get("authvalue").toString();
        String authType = args.get("authtype") == null ? "" : args.get("authtype").toString();
        this.provider.getNearpayLib().authTypeShared = authType;
        this.provider.getNearpayLib().authValueShared = authvalue;
        boolean isAuthValidated = this.provider.getNearpayLib().isAuthInputValidation(authType, authvalue);
        String localeStr = args.get("locale") != null ? args.get("locale").toString() : "default";
        Locale locale = localeStr.equals("default") ? Locale.getDefault() : Locale.getDefault();
        String environmentStr = args.get("environment") == null ? "sandbox"
                : args.get("environment").toString();
        Environments env = environmentStr.equals("sandbox") ? Environments.SANDBOX
                : environmentStr.equals("production") ? Environments.PRODUCTION : Environments.TESTING;

        Map<String, Object> response;

        if (!isAuthValidated) {
            response = NearpayLib.commonResponse(ErrorStatus.invalid_argument_code,
                    "Authentication parameter missing");
        } else {
            this.provider.getNearpayLib().nearpay = new NearPay(
                    this.provider.getNearpayLib().context,
                    this.provider.getNearpayLib().getAuthType(authType, authvalue),
                    locale,
                    env);

            this.provider.getNearpayLib().nearpayProxy = new NearpayProxy(
                    (Application) this.provider.getNearpayLib().context.getApplicationContext(),
                    this.provider.getNearpayLib().nearpay);

            response = NearpayLib.commonResponse(ErrorStatus.success_code,
                    "NearPay initialized");
        }

        return response;

    }

    @SuppressLint("NewApi")
    @Override
    public void run(Map args, CompletableFuture<Map> promise) {
        promise.complete(doInitialization(args));
    }
}
