package com.reactnative_stripeterminal;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.stripe.stripeterminal.Callback;
import com.stripe.stripeterminal.Cancelable;
import com.stripe.stripeterminal.ConnectionStatus;
import com.stripe.stripeterminal.ConnectionTokenCallback;
import com.stripe.stripeterminal.ConnectionTokenException;
import com.stripe.stripeterminal.ConnectionTokenProvider;
import com.stripe.stripeterminal.DeviceType;
import com.stripe.stripeterminal.DiscoveryConfiguration;
import com.stripe.stripeterminal.DiscoveryListener;
import com.stripe.stripeterminal.LogLevel;
import com.stripe.stripeterminal.PaymentIntent;
import com.stripe.stripeterminal.PaymentIntentCallback;
import com.stripe.stripeterminal.PaymentIntentParameters;
import com.stripe.stripeterminal.PaymentStatus;
import com.stripe.stripeterminal.Reader;
import com.stripe.stripeterminal.ReaderCallback;
import com.stripe.stripeterminal.ReaderDisplayListener;
import com.stripe.stripeterminal.ReaderDisplayMessage;
import com.stripe.stripeterminal.ReaderEvent;
import com.stripe.stripeterminal.ReaderInputOptions;
import com.stripe.stripeterminal.Terminal;
import com.stripe.stripeterminal.TerminalException;
import com.stripe.stripeterminal.TerminalListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.reactnative_stripeterminal.Constants.*;

public class RNStripeTerminalModule extends ReactContextBaseJavaModule implements TerminalListener, ConnectionTokenProvider, ReaderDisplayListener {
    final static String TAG = RNStripeTerminalModule.class.getSimpleName();
    final static String moduleName = "RNStripeTerminal";
    Cancelable lastDiscoverReaderAttempt = null;
    Cancelable lastPaymentAttempt = null;
    PaymentIntent lastPaymentIntent = null;
    ReaderEvent lastReaderEvent=ReaderEvent.CARD_REMOVED;
    ConnectionTokenCallback pendingConnectionTokenCallback = null;
    String lastCurrency = null;
    List<Reader> discoveredReadersList = null;


    public RNStripeTerminalModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    ReactContext getContext(){
        return getReactApplicationContext();
    }

    @Override
    public String getName() {
        return moduleName;
    }

    @Nullable
    @Override
    public Map<String, Object> getConstants() {
        return constants;
    }

    public void sendEventWithName(String eventName, WritableMap eventData){
        getContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, eventData);
    }

    public void sendEventWithName(String eventName, WritableArray eventData){
        getContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, eventData);
    }

    WritableMap serializeReader(Reader reader) {
        WritableMap writableMap = Arguments.createMap();
        if(reader!=null) {
            writableMap.putDouble(BATTERY_LEVEL, reader.getBatteryLevel());
            writableMap.putInt(DEVICE_TYPE, reader.getDeviceType().ordinal());
            writableMap.putString(SERIAL_NUMBER, reader.getSerialNumber());
            writableMap.putString(DEVICE_SOFTWARE_VERSION, reader.getSoftwareVersion());
        }
        return writableMap;
    }

    WritableMap serializePaymentIntent(PaymentIntent paymentIntent,String currency){
        WritableMap paymentIntentMap = Arguments.createMap();
        paymentIntentMap.putString(STRIPE_ID,paymentIntent.getId());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZ");
        paymentIntentMap.putString(CREATED,simpleDateFormat.format(new Date(paymentIntent.getCreated())));
        paymentIntentMap.putInt(STATUS,paymentIntent.getStatus().ordinal());
        paymentIntentMap.putInt(AMOUNT,paymentIntent.getAmount());
        paymentIntentMap.putString(CURRENCY,currency);
        WritableMap metaDataMap = Arguments.createMap();
        if(paymentIntent.getMetadata()!=null){
            for(String key:paymentIntent.getMetadata().keySet()){
                metaDataMap.putString(key,String.valueOf(paymentIntent.getMetadata().get(key)));
            }
        }
        paymentIntentMap.putMap(METADATA,metaDataMap);
        return paymentIntentMap;
    }

    void abortPreviousDiscoverReadersCall(){
        if (lastDiscoverReaderAttempt != null && !lastDiscoverReaderAttempt.isCompleted()) {
            Callback cancellationCallback = new Callback() {
                @Override
                public void onSuccess() {} //Do Nothing

                @Override
                public void onFailure(@Nonnull TerminalException e) {
                    if(e!=null){
                        Logger.log(TAG,"Error in aborting previous discover reader request.\n Error : "+e.getErrorMessage());
                    }
                }
            };

            lastDiscoverReaderAttempt.cancel(cancellationCallback);
        }
    }

    void abortPrevCreatePaymentRequest(){
        //Todo:Abort payment req here
    }

    void abortInstallUpdate(){
        //Todo: Abort install update req
    }

    @ReactMethod
    public void discoverReaders(int deviceType, int method, boolean simulated) {
        try {
            DeviceType devType = DeviceType.values()[deviceType];
            DiscoveryConfiguration discoveryConfiguration = new DiscoveryConfiguration(0, devType, simulated);
            Callback statusCallback = new Callback() {

                @Override
                public void onSuccess() {
                    WritableMap readerCompletionResponse = Arguments.createMap();
                    lastDiscoverReaderAttempt = null;
                    sendEventWithName(EVENT_READER_DISCOVERY_COMPLETION,readerCompletionResponse);
                }

                @Override
                public void onFailure(@Nonnull TerminalException e) {
                    WritableMap errorMap = Arguments.createMap();
                    if(e!=null) {
                        lastDiscoverReaderAttempt = null;
                        errorMap.putString(ERROR, e.getErrorMessage());
                    }else{
                        errorMap.putString(ERROR, "Something went wrong");
                    }

                    sendEventWithName(EVENT_READER_DISCOVERY_COMPLETION,errorMap);
                }
            };

            DiscoveryListener discoveryListener = new DiscoveryListener() {
                @Override
                public void onUpdateDiscoveredReaders(@Nonnull List<Reader> list) {
                    discoveredReadersList = list;
                    WritableArray readersDiscoveredArr = Arguments.createArray();
                    for(Reader reader : list){
                        if(reader!=null){
                            readersDiscoveredArr.pushMap(serializeReader(reader));
                        }
                    }

                    sendEventWithName(EVENT_READERS_DISCOVERED,readersDiscoveredArr);
                }
            };

            abortPreviousDiscoverReadersCall();
            lastDiscoverReaderAttempt = Terminal.getInstance().discoverReaders(discoveryConfiguration, discoveryListener, statusCallback);

        }catch (Exception e){
            e.printStackTrace();

            if(e.getMessage()!=null) {
                WritableMap writableMap = Arguments.createMap();
                writableMap.putString(ERROR,e.getMessage());
                sendEventWithName(EVENT_READER_DISCOVERY_COMPLETION, writableMap);
            }
        }
    }

    @ReactMethod
    public void initialize() {
        pendingConnectionTokenCallback = null;
        abortPreviousDiscoverReadersCall();
        abortPrevCreatePaymentRequest();
        abortInstallUpdate();

        LogLevel logLevel = LogLevel.VERBOSE;
        ConnectionTokenProvider tokenProvider = this;
        TerminalListener terminalListener = this;

        try {
            Terminal.initTerminal(getContext().getApplicationContext(), logLevel, tokenProvider, terminalListener);
            lastReaderEvent = ReaderEvent.CARD_REMOVED;
        } catch (TerminalException e) {
            e.printStackTrace();
        }
    }

    @ReactMethod
    public void setConnectionToken(String token,String errorMsg){
        if(pendingConnectionTokenCallback!=null){
            if(errorMsg!=null && !errorMsg.trim().isEmpty()){
                pendingConnectionTokenCallback.onFailure(new ConnectionTokenException(errorMsg));
            }else{
                pendingConnectionTokenCallback.onSuccess(token);
            }
        }

        pendingConnectionTokenCallback = null;
    }

    @ReactMethod
    public void createPayment(final ReadableMap options) {
        PaymentIntentCallback paymentIntentCallback = new PaymentIntentCallback() {
            @Override
            public void onSuccess(@Nonnull final PaymentIntent paymentIntent) {
                lastPaymentAttempt = Terminal.getInstance().collectPaymentMethod(paymentIntent, RNStripeTerminalModule.this
                        , new PaymentIntentCallback() {
                            @Override
                            public void onSuccess(@Nonnull final PaymentIntent collectedIntent) {
                                lastPaymentAttempt = null;
                                Terminal.getInstance().processPayment(collectedIntent, new PaymentIntentCallback() {
                                    @Override
                                    public void onSuccess(@Nonnull PaymentIntent confirmedIntent) {
                                        WritableMap intentMap = Arguments.createMap();
                                        String currency = "";
                                        if (options != null && options.hasKey(CURRENCY)) {
                                            currency = options.getString(CURRENCY);
                                        }
                                        intentMap.putMap(INTENT, serializePaymentIntent(confirmedIntent, currency));
                                        sendEventWithName(EVENT_PAYMENT_CREATION, intentMap);
                                    }

                                    @Override
                                    public void onFailure(@Nonnull TerminalException e) {
                                        WritableMap errorMap = Arguments.createMap();
                                        errorMap.putString(ERROR, e.getErrorMessage());
                                        errorMap.putInt(CODE, e.getErrorCode().ordinal());
                                        String currency = "";
                                        if (options != null && options.hasKey(CURRENCY)) {
                                            currency = options.getString(CURRENCY);
                                        }
                                        errorMap.putMap(INTENT, serializePaymentIntent(collectedIntent, currency));
                                        sendEventWithName(EVENT_PAYMENT_CREATION, errorMap);
                                    }
                                });
                            }

                            @Override
                            public void onFailure(@Nonnull TerminalException e) {
                                lastPaymentAttempt = null;
                                WritableMap collectionErrorMap = Arguments.createMap();
                                collectionErrorMap.putString(ERROR, e.getErrorMessage());
                                collectionErrorMap.putInt(CODE, e.getErrorCode().ordinal());
                                String currency = "";
                                if (options != null && options.hasKey(CURRENCY)) {
                                    currency = options.getString(CURRENCY);
                                }
                                collectionErrorMap.putMap(INTENT, serializePaymentIntent(paymentIntent, currency));
                                sendEventWithName(EVENT_PAYMENT_CREATION, collectionErrorMap);
                            }
                        });
            }

            @Override
            public void onFailure(@Nonnull TerminalException e) {
                WritableMap paymentCreationMap = Arguments.createMap();
                paymentCreationMap.putString(ERROR, e.getErrorMessage());
                paymentCreationMap.putInt(CODE, e.getErrorCode().ordinal());
                sendEventWithName(EVENT_PAYMENT_CREATION, paymentCreationMap);
            }
        };

        String paymentIntent = options.getString(PAYMENT_INTENT);
        if (paymentIntent != null && !paymentIntent.trim().isEmpty()) {
            Terminal.getInstance().retrievePaymentIntent(paymentIntent, paymentIntentCallback);
        } else {
            int amount = options.getInt(AMOUNT);
            String currency = options.getString(CURRENCY);
            PaymentIntentParameters.Builder paymentIntentParamBuilder = new PaymentIntentParameters.Builder();
            paymentIntentParamBuilder.setAmount(amount);
            paymentIntentParamBuilder.setCurrency(currency);
            if (options.hasKey(APPLICATION_FEE_AMOUNT)) {
                int applicationFeeAmount = options.getInt(APPLICATION_FEE_AMOUNT);
                paymentIntentParamBuilder.setApplicationFeeAmount(applicationFeeAmount);
            }

            Terminal.getInstance().createPaymentIntent(paymentIntentParamBuilder.build(), paymentIntentCallback);
        }
    }

    @ReactMethod
    public void createPaymentIntent(ReadableMap options){
        int amount = 0;
        String currency = "";
        int applicationFeeAmount = 0;

        if(options!=null){
            if(options.hasKey(AMOUNT))
                amount= options.getInt(AMOUNT);

            if(options.hasKey(CURRENCY))
                currency = options.getString(CURRENCY);

            if(options.hasKey(APPLICATION_FEE_AMOUNT))
                applicationFeeAmount = options.getInt(APPLICATION_FEE_AMOUNT);

            lastCurrency = currency;
        }

        PaymentIntentParameters.Builder paramsBuilder = new PaymentIntentParameters.Builder();
        paramsBuilder.setAmount(amount)
        .setCurrency(currency)
        .setApplicationFeeAmount(applicationFeeAmount);

        Terminal.getInstance().createPaymentIntent(paramsBuilder.build(), new PaymentIntentCallback() {
            @Override
            public void onSuccess(@Nonnull PaymentIntent paymentIntent) {
                lastPaymentIntent  = paymentIntent;
                WritableMap paymentIntentCreateRespMap = Arguments.createMap();
                paymentIntentCreateRespMap.putMap(INTENT,serializePaymentIntent(paymentIntent,lastCurrency)); //No currency for android
                sendEventWithName(EVENT_PAYMENT_INTENT_CREATION, paymentIntentCreateRespMap);
            }

            @Override
            public void onFailure(@Nonnull TerminalException e) {
                lastPaymentIntent = null;
                WritableMap paymentIntentCreateRespMap = Arguments.createMap();
                paymentIntentCreateRespMap.putString(ERROR,e.getErrorMessage());
                sendEventWithName(EVENT_PAYMENT_INTENT_CREATION, paymentIntentCreateRespMap);
            }
        });
    }

    @ReactMethod
    public void retrievePaymentIntent(String clientSecret){
        if(clientSecret!=null) {
            Terminal.getInstance().retrievePaymentIntent(clientSecret, new PaymentIntentCallback() {
                @Override
                public void onSuccess(@Nonnull PaymentIntent paymentIntent) {
                    lastPaymentIntent  = paymentIntent;
                    WritableMap paymentRetrieveRespMap = Arguments.createMap();
                    paymentRetrieveRespMap.putMap(INTENT,serializePaymentIntent(paymentIntent,"")); //No currency for android
                    sendEventWithName(EVENT_PAYMENT_INTENT_RETRIEVAL, paymentRetrieveRespMap);
                }

                @Override
                public void onFailure(@Nonnull TerminalException e) {
                    lastPaymentIntent = null;
                    WritableMap paymentRetrieveRespMap = Arguments.createMap();
                    paymentRetrieveRespMap.putString(ERROR,e.getErrorMessage());
                    sendEventWithName(EVENT_PAYMENT_INTENT_RETRIEVAL, paymentRetrieveRespMap);
                }
            });
        }else{
            WritableMap paymentRetrieveRespMap = Arguments.createMap();
            paymentRetrieveRespMap.putString(ERROR,"Client secret cannot be null");
            sendEventWithName(EVENT_PAYMENT_INTENT_RETRIEVAL, paymentRetrieveRespMap);
        }
    }

    @ReactMethod
    public void cancelPaymentIntent(){
        Terminal.getInstance().cancelPaymentIntent(lastPaymentIntent, new PaymentIntentCallback() {
            @Override
            public void onSuccess(@Nonnull PaymentIntent paymentIntent) {
                WritableMap paymentIntentCancelMap = Arguments.createMap();
                paymentIntentCancelMap.putMap(INTENT,serializePaymentIntent(paymentIntent,lastCurrency));
                sendEventWithName(EVENT_PAYMENT_INTENT_CANCEL,paymentIntentCancelMap);
            }

            @Override
            public void onFailure(@Nonnull TerminalException e) {
                WritableMap errorMap = Arguments.createMap();
                errorMap.putString(ERROR,e.getErrorMessage());
                errorMap.putInt(CODE,e.getErrorCode().ordinal());
                errorMap.putMap(INTENT,serializePaymentIntent(lastPaymentIntent,lastCurrency));
                sendEventWithName(EVENT_PAYMENT_INTENT_CANCEL,errorMap);
            }
        });
    }

    @ReactMethod
    public void processPayment(){
        Terminal.getInstance().processPayment(lastPaymentIntent, new PaymentIntentCallback() {
            @Override
            public void onSuccess(@Nonnull PaymentIntent paymentIntent) {
                lastPaymentIntent = paymentIntent;
                WritableMap processPaymentMap = Arguments.createMap();
                processPaymentMap.putMap(INTENT,serializePaymentIntent(paymentIntent,lastCurrency));
                sendEventWithName(EVENT_PROCESS_PAYMENT,processPaymentMap);
            }

            @Override
            public void onFailure(@Nonnull TerminalException e) {
                WritableMap errorMap = Arguments.createMap();
                errorMap.putString(ERROR,e.getErrorMessage());
                errorMap.putInt(CODE,e.getErrorCode().ordinal());
                errorMap.putString(DECLINE_CODE,e.getApiError().getDeclineCode());
                errorMap.putMap(INTENT,serializePaymentIntent(lastPaymentIntent,lastCurrency));
                sendEventWithName(EVENT_PROCESS_PAYMENT,errorMap);
            }
        });
    }
    
    @ReactMethod
    public void collectPaymentMethod(){
        lastPaymentAttempt = Terminal.getInstance().collectPaymentMethod(lastPaymentIntent, this, new PaymentIntentCallback() {
            @Override
            public void onSuccess(@Nonnull PaymentIntent paymentIntent) {
                lastPaymentIntent = paymentIntent;
                WritableMap collectPaymentMethodMap = Arguments.createMap();
                collectPaymentMethodMap.putMap(INTENT,serializePaymentIntent(paymentIntent,lastCurrency));
                sendEventWithName(EVENT_PAYMENT_METHOD_COLLECTION,collectPaymentMethodMap);
            }

            @Override
            public void onFailure(@Nonnull TerminalException e) {
                WritableMap errorMap = Arguments.createMap();
                errorMap.putString(ERROR,e.getErrorMessage());
                errorMap.putInt(CODE,e.getErrorCode().ordinal());
                errorMap.putMap(INTENT,serializePaymentIntent(lastPaymentIntent,lastCurrency));
                sendEventWithName(EVENT_PAYMENT_METHOD_COLLECTION,errorMap);
            }
        });
    }

    @ReactMethod
    public void connectReader(String serialNumber){
        Reader selectedReader = null;
        if(discoveredReadersList!=null && discoveredReadersList.size()>0){
            for(Reader reader:discoveredReadersList){
                if(reader!=null){
                    if(reader.getSerialNumber().equals(serialNumber)){
                        selectedReader  = reader;
                    }
                }
            }
        }

        if(selectedReader!=null) {
            Terminal.getInstance().connectReader(selectedReader, new ReaderCallback() {
                @Override
                public void onSuccess(@Nonnull Reader reader) {
                    sendEventWithName(EVENT_READER_CONNECTION, serializeReader(reader));
                }

                @Override
                public void onFailure(@Nonnull TerminalException e) {
                    WritableMap errorMap = Arguments.createMap();
                    errorMap.putString(ERROR, e.getErrorMessage());
                    sendEventWithName(EVENT_READER_CONNECTION, errorMap);
                }
            });
        }else{
            WritableMap errorMap = Arguments.createMap();
            errorMap.putString(ERROR, "No reader found with provided serial number");
            sendEventWithName(EVENT_READER_CONNECTION,errorMap);
        }
    }


    @Override
    public void fetchConnectionToken(@Nonnull ConnectionTokenCallback connectionTokenCallback) {
        pendingConnectionTokenCallback = connectionTokenCallback;
        sendEventWithName(EVENT_REQUEST_CONNECTION_TOKEN,Arguments.createMap());
    }

    @Override
    public void onReportLowBatteryWarning() {
       sendEventWithName(EVENT_DID_REPORT_LOW_BATTERY_WARNING,Arguments.createMap());
    }

    @Override
    public void onConnectionStatusChange(@Nonnull ConnectionStatus status) {
        WritableMap statusMap = Arguments.createMap();
        statusMap.putInt(STATUS,status.ordinal());
        sendEventWithName(EVENT_DID_CHANGE_CONNECTION_STATUS,statusMap);
    }

    @Override
    public void onReportReaderEvent(@Nonnull ReaderEvent event) {
        lastReaderEvent = event;
        WritableMap readerEventReportMap = Arguments.createMap();
        readerEventReportMap.putInt(EVENT,event.ordinal());
        readerEventReportMap.putMap(INFO,Arguments.createMap());
        sendEventWithName(EVENT_DID_REPORT_READER_EVENT, readerEventReportMap);
    }

    @Override
    public void onPaymentStatusChange(@Nonnull PaymentStatus status) {
        WritableMap paymentStatusMap = Arguments.createMap();
        paymentStatusMap.putInt(STATUS,status.ordinal());
        sendEventWithName(EVENT_DID_CHANGE_PAYMENT_STATUS,paymentStatusMap);
    }

    @Override
    public void onUnexpectedReaderDisconnect(@Nonnull Reader reader) {
        sendEventWithName(EVENT_DID_REPORT_UNEXPECTED_READER_DISCONNECT,serializeReader(reader));
    }

    @Override
    public void onRequestReaderInput(@Nonnull ReaderInputOptions readerInputOptions) {
        WritableMap readerOptionsMap = Arguments.createMap();
        readerOptionsMap.putString(TEXT,readerInputOptions.toString());
        sendEventWithName(EVENT_DID_REQUEST_READER_INPUT,readerOptionsMap);
    }

    @Override
    public void onRequestReaderDisplayMessage(@Nonnull ReaderDisplayMessage readerDisplayMessage) {
        WritableMap displayMessageMap = Arguments.createMap();
        displayMessageMap.putString(TEXT,readerDisplayMessage.toString());
        sendEventWithName(EVENT_DID_REQUEST_READER_DISPLAY_MESSAGE,displayMessageMap);
    }
}