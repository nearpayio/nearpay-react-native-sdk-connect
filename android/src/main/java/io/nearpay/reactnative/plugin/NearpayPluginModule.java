package io.nearpay.reactnative.plugin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Locale;
import android.content.Context;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.bridge.ReadableMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.nearpay.connect.NearpayConnect;
import io.nearpay.sdk.Environments;
import io.nearpay.sdk.NearPay;
import io.nearpay.sdk.utils.enums.AuthenticationData;
import io.nearpay.sdk.data.models.TransactionReceipt;
import io.nearpay.sdk.utils.enums.PurchaseFailure;
import io.nearpay.sdk.utils.enums.RefundFailure;
import io.nearpay.sdk.utils.enums.StatusCheckError;
import io.nearpay.sdk.utils.listeners.PurchaseListener;
import io.nearpay.sdk.utils.listeners.RefundListener;
import io.nearpay.sdk.utils.enums.ReconcileFailure;
import io.nearpay.sdk.data.models.Merchant;
import io.nearpay.sdk.data.models.LocalizationField;
import io.nearpay.sdk.data.models.NameField;
import io.nearpay.sdk.data.models.LabelField;
import io.nearpay.sdk.data.models.ReconciliationReceipt;
import io.nearpay.sdk.data.models.ReconciliationDetails;
import io.nearpay.sdk.data.models.ReconciliationLabelField;
import io.nearpay.sdk.data.models.ReconciliationSchemes;
import io.nearpay.sdk.utils.listeners.ReconcileListener;
import io.nearpay.sdk.utils.enums.ReversalFailure;
import io.nearpay.sdk.utils.enums.LogoutFailure;
import io.nearpay.sdk.utils.listeners.LogoutListener;
import io.nearpay.sdk.utils.listeners.ReversalListener;
import com.google.gson.Gson;
import java.util.ArrayList;
import io.nearpay.sdk.utils.listeners.SetupListener;
import io.nearpay.sdk.utils.enums.SetupFailure;
import io.nearpay.sdk.data.models.Session;
import io.nearpay.sdk.utils.enums.SessionFailure;
import io.nearpay.sdk.utils.listeners.SessionListener;




@ReactModule(name = NearpayPluginModule.NAME)
public class NearpayPluginModule extends ReactContextBaseJavaModule {
  public static final String NAME = "NearpayPlugin";
  private NearPay nearPay;
  private NearpayConnect nearpayConnect;
  private Context context;
  private String jwtKey = "jwt";
  private String timeOutDefault = "10";
  private String authTypeShared = "";
  private String authValueShared = "";

  public NearpayPluginModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.context = reactContext.getApplicationContext();
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }

  public AuthenticationData getAuthType(String authType, String  inputValue){
    AuthenticationData authentication= authType.equals("userenter")  ? AuthenticationData.UserEnter.INSTANCE :
            authType.equals("email") ? new AuthenticationData.Email(inputValue) :
                    authType.equals("mobile") ? new AuthenticationData.Mobile(inputValue) :
                            authType.equals(jwtKey) ? new AuthenticationData.Jwt(inputValue) :  AuthenticationData.UserEnter.INSTANCE;
  return authentication;
  }

  public boolean isAuthInputValidation(String authType, String  inputValue){
    boolean isAuthValidate= authType.equals("userenter")  ? true : inputValue == "" ? false : true;
    return isAuthValidate;
  }



  @ReactMethod
  public void initialize(ReadableMap params, Promise promise) {
      JSONObject options = NearPayUtil.readableMapToJson(params);
      String authValue = options.optString("authvalue","") ;
      String authType = options.optString("authtype","");
      this.authTypeShared = authType;
      this.authValueShared = authValue;
      boolean isAuthValidated = isAuthInputValidation(authType,authValue);

      String localeStr = options.optString("locale","") ;
      Locale locale = localeStr.equals("default") ? Locale.getDefault() : Locale.getDefault();

      String environmentStr = options.optString("environment","");
      Environments env = environmentStr.equals("sandbox") ? Environments.SANDBOX : environmentStr.equals("production") ? Environments.PRODUCTION : Environments.TESTING;
      
      if(!isAuthValidated) {
          Map<String, Object> paramMap = commonResponse(ErrorStatus.invalid_argument_code,"Authentication parameter missing");
          promise.resolve(toJson(paramMap));
      }else{
          this.nearPay = new NearPay(this.context,getAuthType(authType,authValue), locale, env);
          Map<String, Object> paramMap = commonResponse(ErrorStatus.success_code,"NearPay initialized");
          promise.resolve(toJson(paramMap));
      }

  }





  @ReactMethod
  public void purchase(ReadableMap params, Promise promise) {
    if(this.nearPay != null){
        this.purchaseValidation(params,promise);
    }else{
      Log.i("purchase....", "initialise nil");
      Map<String, Object> paramMap = commonResponse(ErrorStatus.initialise_failed_code,"Plugin Initialise missing, please initialise");
      promise.resolve(toJson(paramMap));
    }
  }

  private void purchaseValidation(ReadableMap params, Promise promise){
    JSONObject options = NearPayUtil.readableMapToJson(params);
    String amountStr = options.optString("amount","");
    String customer_reference_number = options.optString("customer_reference_number","");
    Boolean isEnableUI = options.optBoolean("isEnableUI",true) ;
    Boolean isEnableReverse = options.optBoolean("isEnableReversal",true);

    String authValue = options.optString("authvalue", this.authValueShared);
    String authType = options.optString("authtype",this.authTypeShared);
    boolean isAuthValidated = isAuthInputValidation(authType,authValue);

    String finishTimeout = options.optString("finishTimeout",timeOutDefault) ;
    Long timeout =  Long.valueOf(finishTimeout);

    if(amountStr == ""){
        Map<String, Object> paramMap = commonResponse(ErrorStatus.invalid_argument_code,"Purchase amount parameter missing");
        promise.resolve(toJson(paramMap));
    }
    else if(!isAuthValidated) {
        Map<String, Object> paramMap = commonResponse(ErrorStatus.invalid_argument_code,"Authentication parameter missing");
        promise.resolve(toJson(paramMap));
    }else{
        Long amount =  Long.valueOf(amountStr); 
        doPurchase(amount,customer_reference_number,isEnableUI,isEnableReverse,timeout,authType ,authValue,promise);

    }

  }

  public void doPurchase(Long amount, String customerReferenceNumber,Boolean enableReceiptUi,Boolean isEnableReverse,Long finishTimeOut,String authType, String authValue,Promise promise) {
    this.nearPay.purchase(amount, customerReferenceNumber, enableReceiptUi,isEnableReverse,finishTimeOut, new PurchaseListener() {
      @Override
      public void onPurchaseFailed(@NonNull PurchaseFailure purchaseFailure) {
        if (purchaseFailure instanceof PurchaseFailure.GeneralFailure) {
          // when there is General error .
          Map<String, Object> paramMap = commonResponse(ErrorStatus.general_failure_code,ErrorStatus.general_messsage);
          promise.resolve(toJson(paramMap));
        } else if (purchaseFailure instanceof PurchaseFailure.PurchaseDeclined) {
          // when the payment declined.
          String messageResp = ((PurchaseFailure.PurchaseDeclined) purchaseFailure).toString();
          String message = messageResp != "" && messageResp.length() > 0 ? messageResp : ErrorStatus.purchase_declined_message;
          Map<String, Object> paramMap = commonResponse(ErrorStatus.purchase_declined_code,message);
          promise.resolve(toJson(paramMap));
        } else if (purchaseFailure instanceof PurchaseFailure.PurchaseRejected) {
          // when the payment rejected.
          String messageResp = ((PurchaseFailure.PurchaseRejected) purchaseFailure).toString();
          String message = messageResp != "" && messageResp.length() > 0 ? messageResp : ErrorStatus.purchase_rejected_message;
          Map<String, Object> paramMap = commonResponse(ErrorStatus.purchase_rejected_code,message);
          promise.resolve(toJson(paramMap));
        } else if (purchaseFailure instanceof PurchaseFailure.AuthenticationFailed) {
          String messageResp = ((PurchaseFailure.AuthenticationFailed) purchaseFailure).toString();
          String message = messageResp != "" && messageResp.length() > 0 ? messageResp : ErrorStatus.authentication_failed_message;
          if(authType.equalsIgnoreCase(jwtKey)){
            Log.d("..call jwt call.1111...", authValue);
            nearPay.updateAuthentication(getAuthType(authType, authValue));
          }
          Map<String, Object> paramMap = commonResponse(ErrorStatus.auth_failed_code,message);
          promise.resolve(toJson(paramMap));
        } else if (purchaseFailure instanceof PurchaseFailure.InvalidStatus) {
          // you can get the status using the following code
          String messageResp = ((PurchaseFailure.InvalidStatus) purchaseFailure).toString();
          String message = messageResp != "" && messageResp.length() > 0 ? messageResp : ErrorStatus.invalid_status_messsage;
          Map<String, Object> paramMap = commonResponse(ErrorStatus.invalid_code,message);
          promise.resolve(toJson(paramMap));
        }
      }

      @Override
      public void onPurchaseApproved(@Nullable List<TransactionReceipt> list) {
        List<Map<String, Object>> transactionList = new ArrayList<>();
        for (TransactionReceipt transRecipt : list){
          Map<String, Object> responseDict = getTransactionGetResponse(transRecipt, "Refund Successfull" );
          transactionList.add(responseDict);
        }
        Map<String, Object> responseDict  =  commonResponse(ErrorStatus.success_code,"Payment Success");
        responseDict.put("list",transactionList );
        promise.resolve(toJson(responseDict));

      }
    });

  }




  private static String toJson(Map<String, Object> paramMap) {
    Gson gson = new Gson();
    return gson.toJson(paramMap);
  }

  @ReactMethod
  private void refund(ReadableMap params, Promise promise) {
    if(nearPay != null){
       refundValidation(params, promise);
    }else{
      Log.i("purchase....", "initialise nil");
      Map<String, Object> paramMap = commonResponse(ErrorStatus.initialise_failed_code,"Plugin Initialise missing, please initialise");
      promise.resolve(toJson(paramMap));
    }
  }

  private void refundValidation(ReadableMap params, Promise promise){
    JSONObject options = NearPayUtil.readableMapToJson(params);
    String amountStr = options.optString("amount","") ;
    String reference_retrieval_number = options.optString("transaction_uuid","") ;
    String customer_reference_number = options.optString("customer_reference_number","") ;
    Boolean isEnableUI = options.optBoolean("isEnableUI", true);
    String authValue = options.optString("authvalue",this.authValueShared);
    String authType = options.optString("authtype",this.authTypeShared);
    Boolean isEnableReverse = options.optBoolean("isEnableReversal",true);
    Boolean isEditableReversalUI = options.optBoolean("isEditableReversalUI",true);
    String adminPin = options.optString("adminPin",null);


    String finishTimeout = options.optString("finishTimeout",timeOutDefault);
    Long timeout =  Long.valueOf(finishTimeout);

    boolean isAuthValidated = isAuthInputValidation(authType,authValue);

    if(amountStr == ""){
        Map<String, Object> paramMap = commonResponse(ErrorStatus.invalid_argument_code,"Purchase amount parameter missing");
        promise.resolve(toJson(paramMap));
    }
    else if(reference_retrieval_number == ""){
        Map<String, Object> paramMap = commonResponse(ErrorStatus.invalid_argument_code,"Transaction UUID parameter missing");
        promise.resolve(toJson(paramMap));
    }
    else if(!isAuthValidated) {
        Map<String, Object> paramMap = commonResponse(ErrorStatus.invalid_argument_code,"Authentication parameter missing");
        promise.resolve(toJson(paramMap));
    }else{
        Long amount =  Long.valueOf(amountStr); 
        doRefundAction(amount,reference_retrieval_number, customer_reference_number,isEnableUI,isEnableReverse,isEditableReversalUI,authType,authValue,timeout,adminPin,promise );
    }

  }

  private void doRefundAction(Long amount ,String transactionReferenceRetrievalNumber, String customerReferenceNumber,Boolean enableReceiptUi,Boolean isEnableReverse,Boolean isEditableReversalUI,String authType, String authValue,Long finishTimeOut,String adminPin,Promise promise) {
    nearPay.refund(amount, transactionReferenceRetrievalNumber, customerReferenceNumber, enableReceiptUi,isEnableReverse,isEditableReversalUI,finishTimeOut,adminPin, new RefundListener() {
      @Override
      public void onRefundFailed(@NonNull RefundFailure refundFailure) {

        if (refundFailure instanceof RefundFailure.GeneralFailure) {
          // when there is General error .
          Map<String, Object> paramMap = commonResponse(ErrorStatus.general_failure_code,ErrorStatus.general_messsage);
          promise.resolve(toJson(paramMap));
        } else if (refundFailure instanceof RefundFailure.RefundDeclined) {
          // when the payment declined.
          String messageResp = ((RefundFailure.RefundDeclined) refundFailure).toString();
          String message = messageResp != "" && messageResp.length() > 0 ? messageResp : ErrorStatus.refund_declined_message;
          Map<String, Object> paramMap = commonResponse(ErrorStatus.refund_declined_code,message);
          promise.resolve(toJson(paramMap));
        } else if (refundFailure instanceof RefundFailure.RefundRejected) {
          // when the payment rejected.
          String messageResp = ((RefundFailure.RefundRejected) refundFailure).toString();
          String message = messageResp != "" && messageResp.length() > 0 ? messageResp : ErrorStatus.refund_rejected_message;
          Map<String, Object> paramMap = commonResponse(ErrorStatus.refund_rejected_code,message);
          promise.resolve(toJson(paramMap));
        } else if (refundFailure instanceof RefundFailure.AuthenticationFailed) {
          String messageResp = ((RefundFailure.AuthenticationFailed) refundFailure).toString();
          String message = messageResp != "" && messageResp.length() > 0 ? messageResp : ErrorStatus.authentication_failed_message;
          if(authType.equalsIgnoreCase(jwtKey)){
            Log.d("..call jwt call.1111...", authValue);
            nearPay.updateAuthentication(getAuthType(authType, authValue));
          }
          Map<String, Object> paramMap = commonResponse(ErrorStatus.auth_failed_code,message);
          promise.resolve(toJson(paramMap));
        } else if (refundFailure instanceof RefundFailure.InvalidStatus) {
          // you can get the status using the following code
          String messageResp = ((RefundFailure.InvalidStatus) refundFailure).toString();
          String message = messageResp != "" && messageResp.length() > 0 ? messageResp : ErrorStatus.invalid_status_messsage;
          Map<String, Object> paramMap = commonResponse(ErrorStatus.invalid_code,message);
          promise.resolve(toJson(paramMap));
        }
      }

      @Override
      public void onRefundApproved(@Nullable List<TransactionReceipt> list) {
        List<Map<String, Object>> transactionList = new ArrayList<>();
        for (TransactionReceipt transRecipt : list){
          Map<String, Object> responseDict = getTransactionGetResponse(transRecipt, "Refund Successfull" );
          transactionList.add(responseDict);
        }
        Map<String, Object> responseDict  =  commonResponse(ErrorStatus.success_code,"Refund Success");
        responseDict.put("list",transactionList );
        promise.resolve(toJson(responseDict));
      }
    });

  }



  private static Map<String, Object> commonResponse(int responseCode, String message){
    Map<String, Object> paramMap = new HashMap<>();
    paramMap.put("status", responseCode);
    paramMap.put("message", message);
    return paramMap;
  }


  private static Map<String, Object> getTransactionGetResponse(TransactionReceipt transactionReceipt,String message){
    Map<String, Object> paramMap = new HashMap<>();
    paramMap.put("uuid",transactionReceipt.getTransaction_uuid());
    paramMap.put("start_date",transactionReceipt.getStart_date());
    paramMap.put("start_time",transactionReceipt.getStart_time());
    //paramMap.put("bank_id",transactionReceipt.getBank_id());
    paramMap.put("tid",transactionReceipt.getTid());
    paramMap.put("system_trace_audit_number", transactionReceipt.getSystem_trace_audit_number());
    paramMap.put("pos_software_version_number", transactionReceipt.getPos_software_version_number());
    paramMap.put("retrieval_reference_number", transactionReceipt.getRetrieval_reference_number());
    paramMap.put("pan", transactionReceipt.getPan());
    paramMap.put("card_expiration", transactionReceipt.getCard_expiration());
    paramMap.put("is_approved", transactionReceipt.is_approved());
    paramMap.put("is_refunded", transactionReceipt.is_refunded());
    paramMap.put("end_date", transactionReceipt.getEnd_date());
    paramMap.put("end_time", transactionReceipt.getEnd_time());

    //Add Merchant Details
    Merchant merchant = transactionReceipt.getMerchant();
    LocalizationField addresss = merchant.getAddress();
    LocalizationField name = merchant.getName();
    Map<String, Object> merchantData = new HashMap<>();
    merchantData.put("id",merchant.getId() );
    merchantData.put("category_code",merchant.getCategory_code() );

    Map<String, Object> merchantName = new HashMap<>();
    merchantName.put("arabic",name.getArabic() );
    merchantName.put("english",name.getEnglish() );
    merchantData.put("name", merchantName);

    Map<String, Object> merchantAddress = new HashMap<>();
    merchantAddress.put("arabic",addresss.getArabic() );
    merchantAddress.put("english",addresss.getEnglish() );
    merchantData.put("address", merchantAddress);
    paramMap.put("merchant",merchantData );

    //Add Scheme Details
    NameField scheme = transactionReceipt.getCard_scheme();
    LocalizationField schemeName = scheme.getName();
    Map<String, Object> schemeData = new HashMap<>();
    Map<String, Object> schemeNameData = new HashMap<>();
    schemeNameData.put("arabic",schemeName.getArabic() );
    schemeNameData.put("english",schemeName.getEnglish() );
    schemeData.put("id",scheme.getId());
    schemeData.put("name",schemeNameData);
    paramMap.put("scheme",schemeData );

    //Add Trasaction type Details
    NameField trancation = transactionReceipt.getTransaction_type();
    LocalizationField trasactionName = trancation.getName();
    Map<String, Object> transactionData = new HashMap<>();
    Map<String, Object> transactionNameData = new HashMap<>();
    transactionNameData.put("arabic",trasactionName.getArabic() );
    transactionNameData.put("english",trasactionName.getEnglish() );
    transactionData.put("id",trancation.getId());
    transactionData.put("name",trasactionName);
    paramMap.put("transaction_type",transactionData );

    //Add Amount Authorised Details
    LabelField amountAuth = transactionReceipt.getAmount_authorized();
    LocalizationField amountLabel = amountAuth.getLabel();
    Map<String, Object> amountAuthData = new HashMap<>();
    Map<String, Object> amountAuthNameData = new HashMap<>();
    amountAuthNameData.put("arabic",amountLabel.getArabic() );
    amountAuthNameData.put("english",amountLabel.getEnglish() );
    amountAuthData.put("name",amountAuthNameData);
    amountAuthData.put("value",amountAuth.getValue());
    paramMap.put("amount_authorized",amountAuthData );

    //Add Amount Other Details
    LabelField amountOther = transactionReceipt.getAmount_authorized();
    LocalizationField amountOtherLabel = amountOther.getLabel();
    Map<String, Object> amountOtherAuthData = new HashMap<>();
    Map<String, Object> amountOtherNameData = new HashMap<>();
    amountOtherNameData.put("arabic",amountOtherLabel.getArabic() );
    amountOtherNameData.put("english",amountOtherLabel.getEnglish() );
    amountOtherAuthData.put("name",amountOtherNameData);
    amountOtherAuthData.put("value",amountOther.getValue());
    paramMap.put("amount_other",amountOtherAuthData );

    //Add Currency
    LocalizationField currencyLabel = transactionReceipt.getCurrency();
    Map<String, Object> currencyData = new HashMap<>();
    currencyData.put("arabic",currencyLabel.getArabic() );
    currencyData.put("english",currencyLabel.getEnglish() );
    paramMap.put("currency",currencyData );

    //Add Status Message
    LocalizationField statusMessage = transactionReceipt.getStatus_message();
    Map<String, Object> statusMessageData = new HashMap<>();
    statusMessageData.put("arabic",statusMessage.getArabic() );
    statusMessageData.put("english",statusMessage.getEnglish() );
    paramMap.put("status_message",statusMessageData );

    //Add Status Message
    LabelField approvalCode = transactionReceipt.getApproval_code();
    if(approvalCode != null) {
      LocalizationField approvalLabel = approvalCode.getLabel();
      Map<String, Object> approvalCodeData = new HashMap<>();
      Map<String, Object> approvalCodeNameData = new HashMap<>();
      approvalCodeNameData.put("arabic",approvalLabel.getArabic());
      approvalCodeNameData.put("english",approvalLabel.getEnglish() );
      approvalCodeData.put("value",approvalCode.getValue());
      approvalCodeData.put("label",approvalCodeNameData);
      paramMap.put("approval_code",approvalCodeData );
    }

    //Add Verification Method
    LocalizationField verificationMethod = transactionReceipt.getVerification_method();
    Map<String, Object> verificationMethodData = new HashMap<>();
    verificationMethodData.put("arabic",verificationMethod.getArabic() );
    verificationMethodData.put("english",verificationMethod.getEnglish() );
    paramMap.put("verification_method",verificationMethodData );

    // Recipent Inline one
    Map<String, Object> recipentInlineone = new HashMap<>();
    recipentInlineone.put("arabic",transactionReceipt.getReceipt_line_one().getArabic() );
    recipentInlineone.put("english",transactionReceipt.getReceipt_line_one().getEnglish() );
    paramMap.put("receipt_line_one",recipentInlineone );

    // Recipent Inline one
    Map<String, Object> recipentInlineTwo = new HashMap<>();
    recipentInlineTwo.put("arabic",transactionReceipt.getReceipt_line_two().getArabic() );
    recipentInlineTwo.put("english",transactionReceipt.getReceipt_line_two().getEnglish() );
    paramMap.put("receipt_line_two",recipentInlineTwo );

    // Recipent Thanks Message
    Map<String, Object> thanksMessage = new HashMap<>();
    thanksMessage.put("arabic",transactionReceipt.getThanks_message().getArabic() );
    thanksMessage.put("english",transactionReceipt.getThanks_message().getEnglish() );
    paramMap.put("thanks_message",thanksMessage );

    // Recipent Save Message
    Map<String, Object> saveMessage = new HashMap<>();
    saveMessage.put("arabic",transactionReceipt.getSave_receipt_message().getArabic() );
    saveMessage.put("english",transactionReceipt.getSave_receipt_message().getEnglish() );
    paramMap.put("save_receipt_message",saveMessage );

    paramMap.put("entry_mode",transactionReceipt.getEntry_mode() );
    paramMap.put("action_code",transactionReceipt.getAction_code() );
    paramMap.put("terminal_verification_result",transactionReceipt.getTerminal_verification_result() );
    paramMap.put("application_identifier",transactionReceipt.getApplication_identifier() );
    paramMap.put("transaction_state_information",transactionReceipt.getTransaction_state_information() );
    paramMap.put("cardholader_verfication_result",transactionReceipt.getCardholader_verfication_result() );
    paramMap.put("cryptogram_information_data",transactionReceipt.getCryptogram_information_data() );
    paramMap.put("application_cryptogram",transactionReceipt.getApplication_cryptogram() );
    paramMap.put("kernel_id",transactionReceipt.getKernel_id() );
    paramMap.put("payment_account_reference",transactionReceipt.getPayment_account_reference() );
    paramMap.put("pan_suffix",transactionReceipt.getPan_suffix() );
    paramMap.put("qr_code",transactionReceipt.getQr_code() );
    return paramMap;
  }


  @ReactMethod
  private void reconcile(ReadableMap params, Promise promise){
    Log.i("doReconcile....", "doReconcile.......first....");
    JSONObject options = NearPayUtil.readableMapToJson(params);
    if(this.nearPay != null){
      Boolean isEnableUI = options.optBoolean("isEnableUI", true);
      String authvalue = options.optString("authvalue",this.authValueShared );
      String authType = options.optString("authtype",this.authTypeShared);
      String finishTimeout = options.optString("finishTimeout",timeOutDefault);
      Long timeout =  Long.valueOf(finishTimeout); 
      boolean isAuthValidated = isAuthInputValidation(authType,authvalue);
      String adminPin = options.optString("adminPin",null);


      if(!isAuthValidated) {
          Map<String, Object> paramMap = commonResponse(ErrorStatus.invalid_argument_code,"Authentication parameter missing");
          promise.resolve(toJson(paramMap));
      }else{
          doReconcileAction(isEnableUI,authType,authvalue,timeout,adminPin,promise);
      }

    }else{
        Log.i("purchase....", "initialise nil");
        Map<String, Object> paramMap = commonResponse(ErrorStatus.initialise_failed_code,"Plugin Initialise missing, please initialise");
        promise.resolve(toJson(paramMap));
    }
 
  }

  private void doReconcileAction(Boolean enableReceiptUi, String authType,String inputValue, long finishTimeOut, String adminPin,Promise promise){
        Log.i("doReconcile....", "doReconcile.......first....");
        nearPay.reconcile(enableReceiptUi,adminPin,finishTimeOut, new ReconcileListener() {
            @Override
            public void onReconcileFinished(@Nullable ReconciliationReceipt reconciliationReceipt) {
                // you can use the object to get the reconciliationReceipt data .
                // write your code here
                Map<String, Object> responseDict = reconcileGetResponse(reconciliationReceipt,"Successfull Reconcile");
                promise.resolve(toJson(responseDict));
            }
            @Override
            public void onReconcileFailed(@NonNull ReconcileFailure reconcileFailure) {
                if (reconcileFailure instanceof ReconcileFailure.AuthenticationFailed) {
                    // when the Authentication is failed
                    String messageResp = ((ReconcileFailure.AuthenticationFailed) reconcileFailure).toString();
                    String message = messageResp != "" && messageResp.length() > 0 ? messageResp : ErrorStatus.authentication_failed_message;
                    Map<String, Object> paramMap = commonResponse(ErrorStatus.auth_failed_code,message);
                    promise.resolve(toJson(paramMap));
                    if(authType.equalsIgnoreCase(jwtKey)){
                        Log.d("..call jwt call.1111...", inputValue);
                        nearPay.updateAuthentication(getAuthType(authType, inputValue));
                    }
                }
                else if (reconcileFailure instanceof ReconcileFailure.GeneralFailure){
                    // when there is general error .
                    Map<String, Object> paramMap = commonResponse(ErrorStatus.general_failure_code,ErrorStatus.general_messsage);
                    promise.resolve(toJson(paramMap));
                }
                else if (reconcileFailure instanceof ReconcileFailure.FailureMessage){
                    // when there is FailureMessage
                    Map<String, Object> paramMap = commonResponse(ErrorStatus.failure_code,ErrorStatus.failure_messsage);
                    promise.resolve(toJson(paramMap));
                }
                else if (reconcileFailure instanceof ReconcileFailure.InvalidStatus){
                    // you can get the status using following code
                    String messageResp = ((ReconcileFailure.InvalidStatus) reconcileFailure).toString();
                    String message = messageResp != "" && messageResp.length() > 0 ? messageResp : ErrorStatus.invalid_status_messsage;

                    Map<String, Object> paramMap = commonResponse(ErrorStatus.invalid_code,message);
                    promise.resolve(toJson(paramMap));                    
                }
            }
        });
    }


  private static Map<String, Object> reconcileGetResponse(ReconciliationReceipt reconcileReceipt,String message){
    Map<String, Object> paramMap = new HashMap<>();
    //paramMap.put("udid",reconcileReceipt.getUuid());
    paramMap.put("id",reconcileReceipt.getId());
    paramMap.put("date",reconcileReceipt.getDate());
    paramMap.put("time",reconcileReceipt.getTime());

    //Add blance Details
    LabelField balanceRec = reconcileReceipt.is_balanced();
    LocalizationField labelData = balanceRec.getLabel();
    Map<String, Object> merchantData = new HashMap<>();
    Map<String, Object> balanceData = new HashMap<>();
    merchantData.put("arabic",labelData.getArabic() );
    merchantData.put("english",labelData.getEnglish() );
    balanceData.put("label",merchantData);
    balanceData.put("value",balanceRec.getValue());
    paramMap.put("balanced",balanceData);

    //Add details Details
    ReconciliationDetails details = reconcileReceipt.getDetails();
    ReconciliationLabelField totalLabel = details.getTotal();
    Map<String, Object> outterDict = new HashMap<>();
    Map<String, Object> innerDict = new HashMap<>();
    Map<String, Object> totalLabelDict = new HashMap<>();
    totalLabelDict.put("arabic",totalLabel.getLabel().getArabic() );
    totalLabelDict.put("english",totalLabel.getLabel().getEnglish());
    innerDict.put("label",totalLabelDict);
    innerDict.put("count",totalLabel.getCount());
    innerDict.put("total",totalLabel.getTotal());
    outterDict.put("total", innerDict) ;

    ReconciliationLabelField refundData = details.getRefund();
    Map<String, Object> innerDictrefund = new HashMap<>();
    Map<String, Object> totalLabelDictrefund = new HashMap<>();
    totalLabelDictrefund.put("arabic",refundData.getLabel().getArabic() );
    totalLabelDictrefund.put("english",refundData.getLabel().getEnglish());
    innerDictrefund.put("label",totalLabelDictrefund);
    innerDictrefund.put("count",refundData.getCount());
    innerDictrefund.put("total",refundData.getTotal());
    outterDict.put("refund", innerDictrefund) ;

    ReconciliationLabelField purchaseData = details.getPurchase();
    Map<String, Object> innerDictPurchase = new HashMap<>();
    Map<String, Object> totalLabelDictPurchase = new HashMap<>();
    totalLabelDictPurchase.put("arabic",purchaseData.getLabel().getArabic() );
    totalLabelDictPurchase.put("english",purchaseData.getLabel().getEnglish());
    innerDictPurchase.put("label",totalLabelDictPurchase);
    innerDictPurchase.put("count",purchaseData.getCount());
    innerDictPurchase.put("total",purchaseData.getTotal());
    outterDict.put("purchase", innerDictPurchase) ;

    ReconciliationLabelField refRevData = details.getRefund_reversal();
    Map<String, Object> innerDictRefRev = new HashMap<>();
    Map<String, Object> totalLabelDictRefRev = new HashMap<>();
    totalLabelDictRefRev.put("arabic",refRevData.getLabel().getArabic() );
    totalLabelDictRefRev.put("english",refRevData.getLabel().getEnglish());
    innerDictRefRev.put("label",totalLabelDictRefRev);
    innerDictRefRev.put("count",purchaseData.getCount());
    innerDictRefRev.put("total",purchaseData.getTotal());
    outterDict.put("refund_reversal", innerDictRefRev) ;

    ReconciliationLabelField refPurData = details.getRefund_reversal();
    Map<String, Object> innerDictPurRev = new HashMap<>();
    Map<String, Object> totalLabelDictPurRev = new HashMap<>();
    innerDictPurRev.put("arabic",refPurData.getLabel().getArabic() );
    innerDictPurRev.put("english",refPurData.getLabel().getEnglish());
    totalLabelDictPurRev.put("label",innerDictPurRev);
    totalLabelDictPurRev.put("count",refPurData.getCount());
    totalLabelDictPurRev.put("total",refPurData.getTotal());
    outterDict.put("purchase_reversal", totalLabelDictPurRev) ;

    paramMap.put("details",outterDict);

    //Schem Data
    List<Map<String, Object>> refineMenuList = new ArrayList<>();
    List<ReconciliationSchemes> balanceScheme = reconcileReceipt.getSchemes();
    int index = 0;
    for (ReconciliationSchemes element : balanceScheme){
      if(index == 0){
        LabelField labelnameElemnt = element.getName();
        LocalizationField labelDataElement = labelnameElemnt.getLabel();
        Map<String, Object> otterElement = new HashMap<>();
        Map<String, Object> elementInner = new HashMap<>();
        elementInner.put("arabic",labelDataElement.getArabic() );
        elementInner.put("english",labelDataElement.getEnglish() );
        otterElement.put("label", elementInner);
        otterElement.put("value", labelnameElemnt.getValue());
        Map<String, Object> finalDict = new HashMap<>();
        finalDict.put("name", otterElement);
        refineMenuList.add(finalDict);
      }else if(index == 1){
        Map<String, Object> creditDict = reconcileCommon(element.getPos().getCredit());
        Map<String, Object> debitDict = reconcileCommon(element.getPos().getDebit());
        Map<String, Object> totalDict = reconcileCommon(element.getPos().getTotal());
        Map<String, Object> finalDict = new HashMap<>();
        finalDict.put("credit", creditDict);
        finalDict.put("debit", debitDict);
        finalDict.put("total", totalDict);
        Map<String, Object> finalDictOtter = new HashMap<>();
        finalDictOtter.put("pos",finalDict);
        refineMenuList.add(finalDictOtter);
      }else if(index == 2){
        Map<String, Object> hostcreditDict = reconcileCommon(element.getHost().getCredit());
        Map<String, Object> hostdebitDict = reconcileCommon(element.getHost().getDebit());
        Map<String, Object> hosttotalDict = reconcileCommon(element.getHost().getTotal());
        Map<String, Object> finalDict = new HashMap<>();
        finalDict.put("credit", hostcreditDict);
        finalDict.put("debit", hostdebitDict);
        finalDict.put("total", hosttotalDict);
        Map<String, Object> finalDictOtter = new HashMap<>();
        finalDictOtter.put("host",finalDict);
        refineMenuList.add(finalDictOtter);
      }
      index++;
    }
    paramMap.put("schemes",refineMenuList );

    //Add Currency
    LocalizationField currencyLabel = reconcileReceipt.getCurrency();
    Map<String, Object> currencyData = new HashMap<>();
    currencyData.put("arabic",currencyLabel.getArabic() );
    currencyData.put("english",currencyLabel.getEnglish() );
    paramMap.put("currency",currencyData );

    paramMap.put("status", ErrorStatus.success_code);
    paramMap.put("message", message);
    return paramMap;
  }

  private static Map<String, Object> reconcileCommon(ReconciliationLabelField data){
    LocalizationField labelDataElement = data.getLabel();
    Map<String, Object> otterElement = new HashMap<>();
    Map<String, Object> elementInner = new HashMap<>();
    elementInner.put("arabic",labelDataElement.getArabic() );
    elementInner.put("english",labelDataElement.getEnglish() );
    otterElement.put("label", elementInner);
    otterElement.put("total", data.getTotal());
    otterElement.put("counr", data.getCount());
    return otterElement;
  }

  @ReactMethod
  private void reverse(ReadableMap params, Promise promise){
    if(this.nearPay != null){
      JSONObject options = NearPayUtil.readableMapToJson(params);
      String transactionUuid = options.optString("transaction_uuid","");
      Boolean isEnableUI = options.optBoolean("isEnableUI", true);
      String authvalue = options.optString("authvalue", this.authValueShared);
      String authType = options.optString("authtype", this.authTypeShared);
      String finishTimeout = options.optString("finishTimeout",timeOutDefault);
      Long timeout =  Long.valueOf(finishTimeout); 
      boolean isAuthValidated = isAuthInputValidation(authType,authvalue);

      if(transactionUuid == ""){
          Map<String, Object> paramMap = commonResponse(ErrorStatus.invalid_argument_code,"Transaction UUID parameter missing");
          promise.resolve(toJson(paramMap));
      }else if(!isAuthValidated) {
          Map<String, Object> paramMap = commonResponse(ErrorStatus.invalid_argument_code,"Authentication parameter missing");
          promise.resolve(toJson(paramMap));
      }else{
          doReverseAction(transactionUuid,isEnableUI,authType,authvalue,timeout,promise);
      }

  }else{
      Log.i("purchase....", "initialise nil");
      Map<String, Object> paramMap = commonResponse(ErrorStatus.initialise_failed_code,"Plugin Initialise missing, please initialise");
      promise.resolve(toJson(paramMap));
  }

  }

  private void doReverseAction(String transactionUuid,Boolean enableReceiptUi,String authType, String authValue, Long timeout,Promise promise){
    Log.i("doReverseAction....", "doReverseAction.......first....");
    Long finishTimeOut =  Long.valueOf(timeout);

    nearPay.reverse(transactionUuid, enableReceiptUi,finishTimeOut, new ReversalListener() {

      @Override
      public void onReversalFinished(@Nullable List<TransactionReceipt> list) {
        // you can use "transactionReceipt" to get the transactionReceipt data .
        List<Map<String, Object>> transactionList = new ArrayList<>();
        for (TransactionReceipt transRecipt : list){
          Map<String, Object> responseDict = getTransactionGetResponse(transRecipt, "Refund Successfull" );
          transactionList.add(responseDict);
        }
        Map<String, Object> responseDict  =  commonResponse(ErrorStatus.success_code,"Payment Success");
        responseDict.put("list",transactionList );
        promise.resolve(toJson(responseDict));
      }
      @Override
      public void onReversalFailed(@NonNull ReversalFailure reversalFailure) {
        if (reversalFailure instanceof ReversalFailure.AuthenticationFailed) {
          // when the Authentication is failed
          String messageResp = ((ReversalFailure.AuthenticationFailed) reversalFailure).toString();
          String message = messageResp != "" && messageResp.length() > 0 ? messageResp : ErrorStatus.authentication_failed_message;
          Map<String, Object> paramMap = commonResponse(ErrorStatus.auth_failed_code,message);
          if(authType.equalsIgnoreCase(jwtKey)){
            Log.d("..call jwt call.1111...", authValue);
            nearPay.updateAuthentication(getAuthType(authType, authValue));
          }
          promise.resolve(toJson(paramMap));
        }
        else if (reversalFailure instanceof ReversalFailure.GeneralFailure){
          // when there is general error .
          Map<String, Object> paramMap = commonResponse(ErrorStatus.general_failure_code,ErrorStatus.general_messsage);
          promise.resolve(toJson(paramMap));
        }
        else if (reversalFailure instanceof ReversalFailure.FailureMessage){
          // when there is FailureMessage
          Map<String, Object> paramMap = commonResponse(ErrorStatus.failure_code,ErrorStatus.failure_messsage);
          promise.resolve(toJson(paramMap));
        }
        else if (reversalFailure instanceof ReversalFailure.InvalidStatus){
          // you can get the status using following code
          String messageResp = ((ReversalFailure.InvalidStatus) reversalFailure).toString();
          String message = messageResp != "" && messageResp.length() > 0 ? messageResp : ErrorStatus.invalid_status_messsage;
          Map<String, Object> paramMap = commonResponse(ErrorStatus.invalid_code,message);
          promise.resolve(toJson(paramMap));
        }
      }
    });
  }

  @ReactMethod
  private void logout( Promise promise){
    nearPay.logout(new LogoutListener() {
      @Override
      public void onLogoutCompleted() {
        //write your message here
        Map<String, Object> paramMap = commonResponse(ErrorStatus.success_code,"Logout Successfully");
        promise.resolve(toJson(paramMap));
      }
      @Override
      public void onLogoutFailed(@NonNull LogoutFailure logoutFailure) {
        if (logoutFailure instanceof LogoutFailure.AlreadyLoggedOut) {
          // when the user is already logged out
          Map<String, Object> paramMap = commonResponse(ErrorStatus.logout_already_code,"User already logout");
          promise.resolve(toJson(paramMap));
        }
        else  if (logoutFailure instanceof LogoutFailure.GeneralFailure) {
          // when the error is general error
          Map<String, Object> paramMap = commonResponse(ErrorStatus.general_failure_code,ErrorStatus.general_messsage);
          promise.resolve(toJson(paramMap));
        }
      }
    });
  }

  @ReactMethod
  private void setup(Promise promise){
    if(this.nearPay != null){
      String authvalue = this.authValueShared;
      String authType = this.authTypeShared;
      boolean isAuthValidated = isAuthInputValidation(authType,authvalue);
      if(!isAuthValidated) {
          Map<String, Object> paramMap = commonResponse(ErrorStatus.invalid_argument_code,"Authentication parameter missing");
          promise.resolve(toJson(paramMap));
      }else{
          setupAction(authType,authvalue,promise);
      }
    }else{
      Log.i("purchase....", "initialise nil");
      Map<String, Object> paramMap = commonResponse(ErrorStatus.initialise_failed_code,"Plugin Initialise missing, please initialise");
      promise.resolve(toJson(paramMap));
    } 
  }

  private void setupAction(String authType, String authValue, Promise promise){
    nearPay.setup(new SetupListener() {
      @Override
      public void onSetupCompleted() {
        // when the setup is done successfully
        Map<String, Object> paramMap = commonResponse(ErrorStatus.success_code,"Application setup completed successfully");
        promise.resolve(toJson(paramMap));
      }
      @Override
      public void onSetupFailed(@NonNull SetupFailure setupFailure) {
        if (setupFailure instanceof SetupFailure.AlreadyInstalled) {
          // when the payment plugin is already installed  .
          Map<String, Object> paramMap = commonResponse(ErrorStatus.already_installed_code,"Plugin Application Already Installed");
          promise.resolve(toJson(paramMap));
        }
        else if (setupFailure instanceof SetupFailure.NotInstalled){
          // when the installtion failed .
          Map<String, Object> paramMap = commonResponse(ErrorStatus.not_installed_code,"Plugin Application Installation Failed");
          promise.resolve(toJson(paramMap));
        }
        else if (setupFailure instanceof SetupFailure.AuthenticationFailed){
          String messageResp = ((SetupFailure.AuthenticationFailed) setupFailure).toString();
          String message = messageResp != "" && messageResp.length() > 0 ? messageResp : ErrorStatus.authentication_failed_message;
          // when the Authentication Failed.
          if(authType.equalsIgnoreCase(jwtKey)){
            Log.d("..call jwt call.1111...", authValue);
            nearPay.updateAuthentication(getAuthType(authType, authValue));
            Map<String, Object> paramMap = commonResponse(ErrorStatus.auth_failed_code,message);
            promise.resolve(toJson(paramMap));
          }
          else{
            Map<String, Object> paramMap = commonResponse(ErrorStatus.auth_failed_code,message);
            promise.resolve(toJson(paramMap));
          }

        }
        else if (setupFailure instanceof SetupFailure.InvalidStatus){
          // you can get the status using the following code
          String messageResp = ((SetupFailure.InvalidStatus) setupFailure).toString();
          String message = messageResp != "" && messageResp.length() > 0 ? messageResp : ErrorStatus.invalid_status_messsage;
          Map<String, Object> paramMap = commonResponse(ErrorStatus.invalid_code,message);
          promise.resolve(toJson(paramMap));
        }
      }
    });
  }

  @ReactMethod
  public void session(ReadableMap params, Promise promise) {
      JSONObject options = NearPayUtil.readableMapToJson(params);
      if(options != null){
        String sessionID = options.optString("sessionID","");
        String finishTimeout = options.optString("finishTimeout", timeOutDefault);
        Long timeout =  Long.valueOf(finishTimeout); 
        Boolean isEnableUI = options.optBoolean("isEnableUI", true);
        Boolean isEnableReverse = options.optBoolean("isEnableReversal", true);

        if(sessionID == "") {
            Map<String, Object> paramMap = commonResponse(ErrorStatus.invalid_argument_code,"SessionID parameter missing");
            promise.resolve(toJson(paramMap));
        }else{
            setSession(sessionID,isEnableUI,isEnableReverse,timeout,promise);
        }
      }else{
          Map<String, Object> paramMap = commonResponse(ErrorStatus.invalid_argument_code,"SessionID parameter missing");
          promise.resolve(toJson(paramMap));
      }  
  }

  private void setSession(String sessionID,Boolean enableReceiptUi,Boolean enableReversal, Long finishTimeOut,Promise promise){
    nearPay.session(sessionID, enableReceiptUi, enableReversal, finishTimeOut, new SessionListener() {
    @Override
    public void onSessionClosed(@Nullable Session session) {
        // when the session is closed 
        Map<String, Object> responseDict  =  sessionResponse(session,"Session Closed");
        promise.resolve(toJson(responseDict));
    }
    @Override
    public void onSessionOpen(@Nullable List<TransactionReceipt> list) {
        // when the session is open , you can get the receipt by using TransactionReceipt
        List<Map<String, Object>> transactionList = new ArrayList<>();
        for (TransactionReceipt transRecipt : list){
            Map<String, Object> responseDict = getTransactionGetResponse(transRecipt, "Session Successfull" );
            transactionList.add(responseDict);
        }
        Map<String, Object> responseDict  =  commonResponse(ErrorStatus.success_code,"Session Success");
        responseDict.put("list",transactionList );
        promise.resolve(toJson(responseDict));
    }
    @Override
    public void onSessionFailed(@NonNull SessionFailure sessionFailure) {
        if (sessionFailure instanceof SessionFailure.AuthenticationFailed) {
            // when the authentication is failed
            String messageResp = ((SessionFailure.AuthenticationFailed) sessionFailure).toString();
            String message = messageResp != "" && messageResp.length() > 0 ? messageResp : ErrorStatus.authentication_failed_message;
            Map<String, Object> paramMap = commonResponse(ErrorStatus.auth_failed_code,message);
            promise.resolve(toJson(paramMap));
            if(authTypeShared.equalsIgnoreCase(jwtKey)){
                nearPay.updateAuthentication(getAuthType(authTypeShared, authTypeShared));
            }

        }
        else if (sessionFailure instanceof SessionFailure.GeneralFailure) {
            // when there is general error .
            Map<String, Object> paramMap = commonResponse(ErrorStatus.general_failure_code,ErrorStatus.general_messsage);
            promise.resolve(toJson(paramMap));
        }
        else if (sessionFailure instanceof SessionFailure.FailureMessage) {
            // when there is FailureMessage
            Map<String, Object> paramMap = commonResponse(ErrorStatus.failure_code,ErrorStatus.failure_messsage);
            promise.resolve(toJson(paramMap));
        }
        else if (sessionFailure instanceof SessionFailure.InvalidStatus) {
            // you can get the status using the following code
            String messageResp = ((SessionFailure.InvalidStatus) sessionFailure).toString();
            String message = messageResp != "" && messageResp.length() > 0 ? messageResp : ErrorStatus.invalid_status_messsage;
            Map<String, Object> paramMap = commonResponse(ErrorStatus.invalid_code,message);
            promise.resolve(toJson(paramMap));
        }
    }
});
}

    private static Map<String, Object> sessionResponse(Session session,String message) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("id",session.getId() );
        paramMap.put("status",session.getStatus() );
        paramMap.put("type",session.getType() );
        paramMap.put("client_id",session.getClient_id() );
        paramMap.put("amount",session.getAmount() );
        paramMap.put("expired_at",session.getExpired_at() );
        paramMap.put("reference_id",session.getReference_id() );
        paramMap.put("created_at",session.getCreated_at() );
        paramMap.put("updated_at",session.getUpdated_at() );
        

        //Transaction response
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("id",session.getTransaction().getId() );
        transaction.put("uuid",session.getTransaction().getUuid() );
        transaction.put("amount_authorized",session.getTransaction().getAmount_authorized() );
        transaction.put("currentcy_code",session.getTransaction().getTransaction_currency_code() );
        transaction.put("cardholder_verification_result",session.getTransaction().getCardholder_verification_result() );
        transaction.put("lat",session.getTransaction().getLat() );
        transaction.put("lon",session.getTransaction().getLon() );
        transaction.put("device_id",session.getTransaction().getDevice_id() );
        transaction.put("merchant_id",session.getTransaction().getMerchant_id() );
        transaction.put("transaction_type",session.getTransaction().getTransaction_type() );
        transaction.put("card_scheme_id",session.getTransaction().getCard_scheme_id() );
        transaction.put("system_trace_audit_number",session.getTransaction().getSystem_trace_audit_number() );
        transaction.put("is_approve",session.getTransaction().is_approved() );
        transaction.put("is_reconcilied",session.getTransaction().is_reconcilied() );
        transaction.put("is_reversed",session.getTransaction().is_reversed() );
        transaction.put("user_id",session.getTransaction().getUser_id() );
        transaction.put("customer_reference_number",session.getTransaction().getCustomer_reference_number() );
        transaction.put("pos_confirm",session.getTransaction().getPos_confirmed() );
        transaction.put("created_at",session.getTransaction().getCreated_at() );
        transaction.put("updated_at",session.getTransaction().getUpdated_at() );
        
        //get transaction details
        List<Map<String, Object>> transactionList = new ArrayList<>();
        for (TransactionReceipt transRecipt : session.getTransaction().getReceipts()){
            Map<String, Object> responseDict = getTransactionGetResponse(transRecipt, "Refund Successfull" );
            transactionList.add(responseDict);
        }

        transaction.put("receipts",transactionList);

        // Card Scheme
        LocalizationField cardSchemeTrans = session.getTransaction().getCard_scheme();
        Map<String, Object> cardSchemObject = new HashMap<>();
        cardSchemObject.put("arabic",cardSchemeTrans.getArabic() );
        cardSchemObject.put("english",cardSchemeTrans.getEnglish() );
        transaction.put("card_scheme",cardSchemObject);

        // Card Scheme
        LocalizationField typeTrans = session.getTransaction().getType();
        Map<String, Object> typeTransObject = new HashMap<>();
        typeTransObject.put("arabic",typeTrans.getArabic() );
        typeTransObject.put("english",typeTrans.getEnglish() );
        transaction.put("type",typeTransObject);

        // Card Scheme
        LocalizationField versificatioType = session.getTransaction().getVerification_method();
        Map<String, Object> versificationObject = new HashMap<>();
        versificationObject.put("arabic",versificatioType.getArabic() );
        versificationObject.put("english",versificatioType.getEnglish() );
        transaction.put("verification_method",versificationObject);

        paramMap.put("transaction",transaction);

        paramMap.put("status", ErrorStatus.success_code);
        paramMap.put("message", message);

        return paramMap;
    }

  @ReactMethod
  public void receiptToImage(ReadableMap params, Promise promise) {
      //JSONObject options = NearPayUtil.readableMapToJson(params);
  }

  @ReactMethod
  public void connectInitialise(ReadableMap params, Promise promise) {
      JSONObject options = NearPayUtil.readableMapToJson(params);
      Log.i("param port no.77777...", ""+options);
      if(options != null){
      String portNo = options.optString("port","-1");
      Log.i("param port no....", ""+portNo);
      int portNumber = Integer.parseInt(portNo);
        Log.i("param port no....", ""+portNumber);
        if(nearPay == null){
            Log.i("purchase....", "initialise nil");
            Map<String, Object> paramMap = commonResponse(ErrorStatus.initialise_failed_code,"Plugin Initialise missing, please initialise");
            promise.resolve(toJson(paramMap));
        }
        else if(portNumber <= -1) {
            Map<String, Object> paramMap = commonResponse(ErrorStatus.invalid_argument_code,"Port number parameter missing");
            promise.resolve(toJson(paramMap));
        }else{
            nearpayConnect = new NearpayConnect(this.context,nearPay,portNumber);
            Map<String, Object> paramMap = commonResponse(ErrorStatus.success_code,"NearPay Connect initialized");
            promise.resolve(toJson(paramMap));
        }
      }else{
        Map<String, Object> paramMap = commonResponse(ErrorStatus.invalid_argument_code,"Port number parameter missing");
            promise.resolve(toJson(paramMap));
      }
  }

  @ReactMethod
  public void showConnection( Promise promise) {
        if(nearpayConnect == null){
            Log.i("Connect....", "initialise nil");
            Map<String, Object> paramMap = commonResponse(ErrorStatus.initialise_failed_code,"Plugin Connect missing, please initialise");
            promise.resolve(toJson(paramMap));
        }else{
            nearpayConnect.showConnection();
        }
  }

  @ReactMethod
  public void connectionDisconnect( Promise promise) {
        if(nearpayConnect == null){
            Log.i("Connect....", "initialise nil");
            Map<String, Object> paramMap = commonResponse(ErrorStatus.initialise_failed_code,"Plugin Connect missing, please initialise");
            promise.resolve(toJson(paramMap));
        }else{
            nearpayConnect.connectionDisconnect();
        }
    }
    
  @ReactMethod
  public void getConnectionSession(Promise promise) {
        if(nearpayConnect == null){
            Log.i("Connect....", "initialise nil");
            Map<String, Object> paramMap = commonResponse(ErrorStatus.initialise_failed_code,"Plugin Connect missing, please initialise");
            promise.resolve(toJson(paramMap));
        }else{
            nearpayConnect.getConnectionSession();
        }
  }

}
