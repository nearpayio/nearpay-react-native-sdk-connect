package io.nearpay.reactnative.plugin.operations;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.nearpay.reactnative.plugin.ErrorStatus;
import io.nearpay.reactnative.plugin.NearpayLib;
import io.nearpay.reactnative.plugin.PluginProvider;
import io.nearpay.sdk.data.models.ReconciliationReceipt;
import io.nearpay.sdk.data.models.TransactionReceipt;
import io.nearpay.sdk.utils.ReceiptUtilsKt;
import io.nearpay.sdk.utils.enums.ReconcileFailure;
import io.nearpay.sdk.utils.listeners.ReconcileListener;

public class ReconciliationOperation extends BaseOperation {

        public ReconciliationOperation(PluginProvider provider) {
                super(provider);
        }

        private void doReconcileAction(Map args, CompletableFuture<Map> promise) {
                Boolean enableReceiptUi = (Boolean) args.get("enableReceiptUi");
                Long finishTimeout = (Long) args.get("finishTimeout");
                String adminPin = args.get("adminPin") == null ? null : (String) args.get("adminPin");
                Boolean enableUiDismiss = (Boolean) args.get("enableUiDismiss");

                provider.getNearpayLib().nearpay.reconcile(enableReceiptUi, adminPin, finishTimeout, enableUiDismiss,
                                new ReconcileListener() {
                                        @Override
                                        public void onReconcileFinished(
                                                        @Nullable ReconciliationReceipt reconciliationReceipt) {
                                                List<ReconciliationReceipt> list = new ArrayList();
                                                list.add(reconciliationReceipt);

                                                Map<String, Object> responseDict = NearpayLib.ReconcileResponse(
                                                                ErrorStatus.success_code, null, list);
                                                promise.complete(responseDict);
                                        }

                                        @Override
                                        public void onReconcileFailed(@NonNull ReconcileFailure reconcileFailure) {
                                                int status = ErrorStatus.general_failure_code;
                                                String message = null;
                                                List<TransactionReceipt> receipts = null;

                                                if (reconcileFailure instanceof ReconcileFailure.AuthenticationFailed) {
                                                        status = ErrorStatus.auth_failed_code;
                                                        message = ((ReconcileFailure.AuthenticationFailed) reconcileFailure)
                                                                        .getMessage();
                                                } else if (reconcileFailure instanceof ReconcileFailure.FailureMessage) {
                                                        status = ErrorStatus.failure_code;
                                                        message = ((ReconcileFailure.FailureMessage) reconcileFailure)
                                                                        .getMessage();
                                                } else if (reconcileFailure instanceof ReconcileFailure.InvalidStatus) {
                                                        status = ErrorStatus.invalid_code;
                                                }
                                                Map response = NearpayLib.ApiResponse(status, message, receipts);
                                                promise.complete(response);

                                        }

                                });
        }

        @Override
        public void run(Map args, CompletableFuture<Map> promise) {
                doReconcileAction(args, promise);
        }
}
