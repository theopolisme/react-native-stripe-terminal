package com.reactnative_stripeterminal;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.stripe.stripeterminal.callable.Callback;
import com.stripe.stripeterminal.callable.Cancelable;
import com.stripe.stripeterminal.callable.ConnectionTokenCallback;
import com.stripe.stripeterminal.callable.ConnectionTokenProvider;
import com.stripe.stripeterminal.callable.DiscoveryListener;
import com.stripe.stripeterminal.callable.PaymentIntentCallback;
import com.stripe.stripeterminal.callable.ReaderCallback;
import com.stripe.stripeterminal.callable.ReaderDisplayListener;
import com.stripe.stripeterminal.callable.ReaderSoftwareUpdateCallback;
import com.stripe.stripeterminal.callable.ReaderSoftwareUpdateListener;
import com.stripe.stripeterminal.callable.TerminalListener;
import com.stripe.stripeterminal.log.LogLevel;
import com.stripe.stripeterminal.model.external.ConnectionStatus;
import com.stripe.stripeterminal.model.external.ConnectionTokenException;
import com.stripe.stripeterminal.model.external.DeviceType;
import com.stripe.stripeterminal.model.external.DiscoveryConfiguration;
import com.stripe.stripeterminal.model.external.PaymentIntent;
import com.stripe.stripeterminal.model.external.PaymentIntentParameters;
import com.stripe.stripeterminal.model.external.PaymentStatus;
import com.stripe.stripeterminal.model.external.Reader;
import com.stripe.stripeterminal.model.external.ReaderDisplayMessage;
import com.stripe.stripeterminal.model.external.ReaderEvent;
import com.stripe.stripeterminal.model.external.ReaderInputOptions;
import com.stripe.stripeterminal.model.external.ReaderSoftwareUpdate;
import com.stripe.stripeterminal.model.external.TerminalException;
import com.stripe.stripeterminal.Terminal;

import java.sql.Wrapper;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.reactnative_stripeterminal.Constants.*;

public class RNStripeTerminalModule extends ReactContextBaseJavaModule implements TerminalListener, ConnectionTokenProvider, ReaderDisplayListener, ReaderSoftwareUpdateListener, DiscoveryListener {
    final static String TAG = RNStripeTerminalModule.class.getSimpleName();
    final static String moduleName = "RNStripeTerminal";
    Cancelable pendingDiscoverReaders = null;
    Cancelable pendingCreatePaymentIntent = null;
    PaymentIntent lastPaymentIntent = null;
    ReaderEvent lastReaderEvent=ReaderEvent.CARD_REMOVED;
    ConnectionTokenCallback pendingConnectionTokenCallback = null;
    String lastCurrency = null;
    List<? extends Reader> discoveredReadersList = null;
    ReaderSoftwareUpdate readerSoftwareUpdate;
    Cancelable pendingInstallUpdate = null;

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

    public void sendEventWithName(String eventName, Object eventData){
        getContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, eventData);
    }

    public void sendEventWithName(String eventName, WritableArray eventData){
        getContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, eventData);
    }

    WritableMap serializeUpdate(ReaderSoftwareUpdate readerSoftwareUpdate){
        WritableMap writableMap = Arguments.createMap();
        WritableMap updateMap = Arguments.createMap();

        if(readerSoftwareUpdate!=null){
            ReaderSoftwareUpdate.UpdateTimeEstimate updateTimeEstimate= readerSoftwareUpdate.getTimeEstimate();
            updateMap.putString(ESTIMATED_UPDATE_TIME,updateTimeEstimate.getDescription());
            updateMap.putString(DEVICE_SOFTWARE_VERSION,readerSoftwareUpdate.getVersion());
            writableMap.putMap(UPDATE,updateMap);
        }

        return writableMap;
    }

    WritableMap serializeReader(Reader reader) {
        WritableMap writableMap = Arguments.createMap();
        if(reader!=null) {
            double batteryLevel = 0;
            if(reader.getBatteryLevel()!=null)
                batteryLevel = (double) reader.getBatteryLevel();
            writableMap.putDouble(BATTERY_LEVEL, batteryLevel);

            int readerType = 0;
            if(reader.getDeviceType()!=null)
                readerType = reader.getDeviceType().ordinal();
            writableMap.putInt(DEVICE_TYPE, readerType);

            String serial = "";

            if(reader.getSerialNumber()!=null)
                serial = reader.getSerialNumber();
            writableMap.putString(SERIAL_NUMBER, serial);

            String softwareVersion = "";
            if(reader.getSoftwareVersion()!=null)
                softwareVersion = reader.getSoftwareVersion();
            writableMap.putString(DEVICE_SOFTWARE_VERSION, softwareVersion);
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

    @ReactMethod
    public void discoverReaders(int deviceType, int method, int simulated) {
        boolean isSimulated = simulated == 0?false:true;
        try {
            DeviceType devType = DeviceType.values()[deviceType];
            DiscoveryConfiguration discoveryConfiguration = new DiscoveryConfiguration(0, devType, isSimulated);
            Callback statusCallback = new Callback() {

                @Override
                public void onSuccess() {
                    pendingDiscoverReaders = null;
                    WritableMap readerCompletionResponse = Arguments.createMap();
                    sendEventWithName(EVENT_READER_DISCOVERY_COMPLETION,readerCompletionResponse);
                }

                @Override
                public void onFailure(@Nonnull TerminalException e) {
                    pendingDiscoverReaders = null;
                    WritableMap errorMap = Arguments.createMap();
                    errorMap.putString(ERROR, e.getErrorMessage());
                    sendEventWithName(EVENT_READER_DISCOVERY_COMPLETION,errorMap);
                }
            };

            abortDiscoverReaders();
            pendingDiscoverReaders = Terminal.getInstance().discoverReaders(discoveryConfiguration, this, statusCallback);

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
    public void initialize(com.facebook.react.bridge.Callback callback) {
        try {
            //Check if stripe is initialized
            Terminal.getInstance();

            WritableMap writableMap = Arguments.createMap();
            writableMap.putBoolean("isInitialized", true);
            callback.invoke(writableMap);
            return;
        }catch (IllegalStateException e){ }

        pendingConnectionTokenCallback = null;
        abortDiscoverReaders();
        abortCreatePayment();
        abortInstallUpdate();

        LogLevel logLevel = LogLevel.VERBOSE;
        ConnectionTokenProvider tokenProvider = this;
        TerminalListener terminalListener = this;
        String err = "";
        boolean isInit =false;
        try {
            Terminal.initTerminal(getContext().getApplicationContext(), logLevel, tokenProvider, terminalListener);
            lastReaderEvent = ReaderEvent.CARD_REMOVED;
            isInit = true;
        } catch (TerminalException e) {
            e.printStackTrace();
            err = e.getErrorMessage();
            isInit = false;
        } catch (IllegalStateException ex){
            ex.printStackTrace();
            err=  ex.getMessage();
            isInit = true;
        }

        WritableMap writableMap = Arguments.createMap();
        writableMap.putBoolean("isInitialized", isInit);

        if(!isInit) {
            writableMap.putString(ERROR, err);
        }

        callback.invoke(writableMap);
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
                pendingCreatePaymentIntent = Terminal.getInstance().collectPaymentMethod(paymentIntent, RNStripeTerminalModule.this
                        , new PaymentIntentCallback() {
                            @Override
                            public void onSuccess(@Nonnull final PaymentIntent collectedIntent) {
                                pendingCreatePaymentIntent = null;
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
                                pendingCreatePaymentIntent = null;
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

        String paymentIntent = null;
        if(options.hasKey(PAYMENT_INTENT))
            paymentIntent = options.getString(PAYMENT_INTENT);

        if (paymentIntent != null && !paymentIntent.trim().isEmpty()) {
            Terminal.getInstance().retrievePaymentIntent(paymentIntent, paymentIntentCallback);
        } else {
            PaymentIntentParameters.Builder paymentIntentParamBuilder = getPaymentParams(options);
            Terminal.getInstance().createPaymentIntent(paymentIntentParamBuilder.build(), paymentIntentCallback);
        }
    }

    private PaymentIntentParameters.Builder getPaymentParams(ReadableMap options){
        PaymentIntentParameters.Builder paymentIntentParamBuilder = new PaymentIntentParameters.Builder();
        if(options!=null) {
            if (options.hasKey(AMOUNT)) {
                paymentIntentParamBuilder.setAmount(options.getInt(AMOUNT));
            }

            if (options.hasKey(CURRENCY)) {
                paymentIntentParamBuilder.setCurrency(options.getString(CURRENCY));
            }

            if (options.hasKey(APPLICATION_FEE_AMOUNT)) {
                paymentIntentParamBuilder.setApplicationFeeAmount(options.getInt(APPLICATION_FEE_AMOUNT));
            }

            if (options.hasKey(ON_BEHALF_OF)) {
                paymentIntentParamBuilder.setOnBehalfOf(options.getString(ON_BEHALF_OF));
            }

            if (options.hasKey(TRANSFER_DATA_DESTINATION)) {
                paymentIntentParamBuilder.setTransferDataDestination(options.getString(TRANSFER_DATA_DESTINATION));
            }

            if (options.hasKey(TRANSFER_GROUP)) {
                paymentIntentParamBuilder.setTransferGroup(TRANSFER_GROUP);
            }

            if (options.hasKey(CUSTOMER)) {
                paymentIntentParamBuilder.setCustomer(options.getString(CUSTOMER));
            }

            if (options.hasKey(DESCRIPTION)) {
                paymentIntentParamBuilder.setDescription(options.getString(DESCRIPTION));
            }

            if (options.hasKey(STATEMENT_DESCRIPTOR)) {
                paymentIntentParamBuilder.setStatementDescriptor(options.getString(STATEMENT_DESCRIPTOR));
            }

            if (options.hasKey(RECEIPT_EMAIL)) {
                paymentIntentParamBuilder.setReceiptEmail(options.getString(RECEIPT_EMAIL));
            }

            if (options.hasKey(METADATA)) {
                ReadableMap map = options.getMap(METADATA);
                HashMap<String, String> metaDataMap = new HashMap<>();

                if (map != null) {
                    ReadableMapKeySetIterator iterator = options.keySetIterator();
                    while (iterator.hasNextKey()) {
                        String key = iterator.nextKey();
                        String val = options.getString(key);
                        metaDataMap.put(key, val);
                    }
                }

                paymentIntentParamBuilder.setMetadata(metaDataMap);
            }
        }

        return paymentIntentParamBuilder;
    }

    @ReactMethod
    public void createPaymentIntent(ReadableMap options){
        if(options!=null){
            if(options.hasKey(CURRENCY))
                lastCurrency = options.getString(CURRENCY);
        }

        PaymentIntentParameters.Builder paramsBuilder = getPaymentParams(options);

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
        pendingCreatePaymentIntent = Terminal.getInstance().collectPaymentMethod(lastPaymentIntent, this, new PaymentIntentCallback() {
            @Override
            public void onSuccess(@Nonnull PaymentIntent paymentIntent) {
                pendingCreatePaymentIntent = null;
                lastPaymentIntent = paymentIntent;
                WritableMap collectPaymentMethodMap = Arguments.createMap();
                collectPaymentMethodMap.putMap(INTENT,serializePaymentIntent(paymentIntent,lastCurrency));
                sendEventWithName(EVENT_PAYMENT_METHOD_COLLECTION,collectPaymentMethodMap);
            }

            @Override
            public void onFailure(@Nonnull TerminalException e) {
                pendingCreatePaymentIntent = null;
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

    @ReactMethod
    public void disconnectReader(){
       if(Terminal.getInstance().getConnectedReader()==null){
           sendEventWithName(EVENT_READER_DISCONNECTION_COMPLETION,Arguments.createMap());
       }else{
           Terminal.getInstance().disconnectReader(new Callback() {
               @Override
               public void onSuccess() {
                   sendEventWithName(EVENT_READER_DISCONNECTION_COMPLETION,Arguments.createMap());
               }

               @Override
               public void onFailure(@Nonnull TerminalException e) {
                    WritableMap errorMap = Arguments.createMap();
                    errorMap.putString(ERROR,e.getErrorMessage());
                    sendEventWithName(EVENT_READER_DISCONNECTION_COMPLETION,errorMap);
               }
           });
       }
    }

    @ReactMethod
    public void getLastReaderEvent(){
        sendEventWithName(EVENT_LAST_READER_EVENT,new Integer(lastReaderEvent.ordinal()));
    }

    @ReactMethod
    public void getConnectedReader(){
        Reader reader = Terminal.getInstance().getConnectedReader();
        sendEventWithName(EVENT_CONNECTED_READER,serializeReader(reader));
    }

    @ReactMethod
    public void abortDiscoverReaders(){
        if(pendingDiscoverReaders!=null && !pendingDiscoverReaders.isCompleted()){
            pendingDiscoverReaders.cancel(new Callback() {
                @Override
                public void onSuccess() {
                    pendingDiscoverReaders = null;
                    sendEventWithName(EVENT_ABORT_DISCOVER_READER_COMPLETION,Arguments.createMap());
                }

                @Override
                public void onFailure(@Nonnull TerminalException e) {
                    WritableMap errorMap  = Arguments.createMap();
                    errorMap.putString(ERROR,e.getErrorMessage());
                    sendEventWithName(EVENT_ABORT_DISCOVER_READER_COMPLETION,errorMap);
                }
            });
        }else{
            sendEventWithName(EVENT_ABORT_DISCOVER_READER_COMPLETION,Arguments.createMap());
        }
    }

    @ReactMethod
    public void abortCreatePayment(){
        if(pendingCreatePaymentIntent!=null && !pendingCreatePaymentIntent.isCompleted()){
            pendingCreatePaymentIntent.cancel(new Callback() {
                @Override
                public void onSuccess() {
                    pendingCreatePaymentIntent = null;
                    sendEventWithName(EVENT_ABORT_CREATE_PAYMENT_COMPLETION,Arguments.createMap());
                }

                @Override
                public void onFailure(@Nonnull TerminalException e) {
                    WritableMap errorMap = Arguments.createMap();
                    errorMap.putString(ERROR,e.getErrorMessage());
                    sendEventWithName(EVENT_ABORT_CREATE_PAYMENT_COMPLETION,errorMap);
                }
            });
        }else{
            sendEventWithName(EVENT_ABORT_CREATE_PAYMENT_COMPLETION,Arguments.createMap());
        }
    }

    @ReactMethod
    public void clearCachedCredentials(){
        Terminal.getInstance().clearCachedCredentials();
    }

    @ReactMethod
    public void abortInstallUpdate(){
        if(pendingInstallUpdate!=null && !pendingInstallUpdate.isCompleted()){
            pendingInstallUpdate.cancel(new Callback() {
                @Override
                public void onSuccess() {
                    pendingInstallUpdate = null;
                    sendEventWithName(EVENT_ABORT_INSTALL_COMPLETION,Arguments.createMap());
                }

                @Override
                public void onFailure(@Nonnull TerminalException e) {
                    WritableMap errorMap = Arguments.createMap();
                    errorMap.putString(ERROR,e.getErrorMessage());
                    sendEventWithName(EVENT_ABORT_INSTALL_COMPLETION,errorMap);
                }
            });
        }else{
            sendEventWithName(EVENT_ABORT_INSTALL_COMPLETION,Arguments.createMap());
        }
    }

    @ReactMethod
    public void installUpdate(){
        pendingInstallUpdate = Terminal.getInstance().installUpdate(readerSoftwareUpdate,this, new Callback() {
            @Override
            public void onSuccess() {
                sendEventWithName(EVENT_UPDATE_INSTALL,Arguments.createMap());
                readerSoftwareUpdate = null;
            }

            @Override
            public void onFailure(@Nonnull TerminalException e) {
                WritableMap errorMap = Arguments.createMap();
                errorMap.putString(ERROR,e.getErrorMessage());
                sendEventWithName(EVENT_UPDATE_INSTALL,errorMap);
            }
        });
    }

    @ReactMethod
    public void checkForUpdate(){
        Terminal.getInstance().checkForUpdate(new ReaderSoftwareUpdateCallback() {
            @Override
            public void onSuccess(@Nullable ReaderSoftwareUpdate readerSoftwareUpdate) {
                RNStripeTerminalModule.this.readerSoftwareUpdate = readerSoftwareUpdate;
                sendEventWithName(EVENT_UPDATE_CHECK,serializeUpdate(readerSoftwareUpdate));
            }

            @Override
            public void onFailure(@Nonnull TerminalException e) {
                WritableMap errorMap = Arguments.createMap();
                errorMap.putString(ERROR,e.getErrorMessage());
                sendEventWithName(EVENT_UPDATE_CHECK,errorMap);
            }
        });
    }

    @ReactMethod
    public void getConnectionStatus(){
        ConnectionStatus status = Terminal.getInstance().getConnectionStatus();
        WritableMap statusMap = Arguments.createMap();
        statusMap.putInt(EVENT_CONNECTION_STATUS,status.ordinal());
    }

    @ReactMethod
    public void getPaymentStatus(){
        PaymentStatus status = Terminal.getInstance().getPaymentStatus();
        WritableMap statusMap = Arguments.createMap();
        statusMap.putInt(EVENT_PAYMENT_STATUS,status.ordinal());
    }

    @Override
    public void onUpdateDiscoveredReaders(@Nonnull List<? extends Reader> list) {
        discoveredReadersList = list;
        WritableArray readersDiscoveredArr = Arguments.createArray();
        for(Reader reader : list){
            if(reader!=null){
                readersDiscoveredArr.pushMap(serializeReader(reader));
            }
        }

        sendEventWithName(EVENT_READERS_DISCOVERED,readersDiscoveredArr);
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
        Map<String, String> readerDisplayMessageToStringMap = new HashMap<String, String>() {{
            put(ReaderDisplayMessage.RETRY_CARD, "RetryCard");
            put(ReaderDisplayMessage.INSERT_CARD, "InsertCard");
            put(ReaderDisplayMessage.INSERT_OR_SWIPE_CARD, "InsertOrSwipeCard");
            put(ReaderDisplayMessage.SWIPE_CARD, "SwipeCard");
            put(ReaderDisplayMessage.REMOVE_CARD, "RemoveCard");
            put(ReaderDisplayMessage.MULTIPLE_CONTACTLESS_CARDS_DETECTED, "MultipleContactlessCardsDetected");
            put(ReaderDisplayMessage.TRY_ANOTHER_READ_METHOD, "TryAnotherReadMethod");
            put(ReaderDisplayMessage.TRY_ANOTHER_CARD, "TryAnotherCard");
        }};

        sendEventWithName(EVENT_DID_REQUEST_READER_DISPLAY_MESSAGE, (String)readerDisplayMessageToStringMap.get(readerDisplayMessage));
    }

    @Override
    public void onReportReaderSoftwareUpdateProgress(float v) {
        sendEventWithName(EVENT_READER_SOFTWARE_UPDATE_PROGRESS,new Float(v));
    }
}
