package com.reactnative_stripeterminal;

import android.telecom.Call;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
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
import com.stripe.stripeterminal.PaymentStatus;
import com.stripe.stripeterminal.Reader;
import com.stripe.stripeterminal.ReaderEvent;
import com.stripe.stripeterminal.Terminal;
import com.stripe.stripeterminal.TerminalException;
import com.stripe.stripeterminal.TerminalListener;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.reactnative_stripeterminal.Constants.*;

public class RNStripeTerminalModule extends ReactContextBaseJavaModule implements TerminalListener, ConnectionTokenProvider {
    final static String TAG = RNStripeTerminalModule.class.getSimpleName();
    final static String moduleName = "RNStripeTerminal";
    Cancelable lastDiscoverReaderAttempt = null;
    ReaderEvent lastReaderEvent=ReaderEvent.CARD_REMOVED;
    ConnectionTokenCallback pendingConnectionTokenCallback = null;

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
                WritableMap readerCompletionResponse = Arguments.createMap();

                @Override
                public void onSuccess() {
                    sendEventWithName(EVENT_READER_DISCOVERY_COMPLETION,readerCompletionResponse);
                }

                @Override
                public void onFailure(@Nonnull TerminalException e) {
                    if(e!=null)
                        readerCompletionResponse.putString(ERROR,e.getErrorMessage());
                }
            };

            DiscoveryListener discoveryListener = new DiscoveryListener() {
                @Override
                public void onUpdateDiscoveredReaders(@Nonnull List<Reader> list) {
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

    @Override
    public void fetchConnectionToken(@Nonnull ConnectionTokenCallback connectionTokenCallback) {
        pendingConnectionTokenCallback = connectionTokenCallback;
        sendEventWithName(EVENT_REQUEST_CONNECTION_TOKEN,Arguments.createMap());
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
}