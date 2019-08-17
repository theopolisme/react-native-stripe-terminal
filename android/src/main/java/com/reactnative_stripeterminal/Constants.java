package com.reactnative_stripeterminal;

import android.net.wifi.aware.DiscoverySession;

import com.stripe.stripeterminal.ConnectionStatus;
import com.stripe.stripeterminal.DeviceType;
import com.stripe.stripeterminal.PaymentIntent;
import com.stripe.stripeterminal.PaymentStatus;
import com.stripe.stripeterminal.ReaderEvent;

import java.util.HashMap;

public class Constants {
    public static final HashMap<String,Object> constants = new HashMap<String,Object>();
    public static final String EVENT_READER_DISCOVERY_COMPLETION = "readerDiscoveryCompletion";
    public static final String EVENT_READERS_DISCOVERED = "readersDiscovered";
    public static final String EVENT_LAST_READER_EVENT = "lastReaderEvent";
    public static final String EVENT_REQUEST_CONNECTION_TOKEN = "requestConnectionToken";
    public static final String EVENT_DID_REPORT_LOW_BATTERY_WARNING = "didReportLowBatteryWarning";
    public static final String EVENT_DID_CHANGE_CONNECTION_STATUS = "didChangeConnectionStatus";
    public static final String EVENT_DID_REPORT_READER_EVENT = "didReportReaderEvent";
    public static final String EVENT_DID_REPORT_UNEXPECTED_READER_DISCONNECT="didReportUnexpectedReaderDisconnect";
    public static final String EVENT_DID_CHANGE_PAYMENT_STATUS = "didChangePaymentStatus";
    public static final String EVENT_PAYMENT_CREATION = "paymentCreation";
    public static final String EVENT_DID_REQUEST_READER_DISPLAY_MESSAGE = "didRequestReaderDisplayMessage";
    public static final String EVENT_DID_REQUEST_READER_INPUT = "didRequestReaderInput";
    public static final String EVENT_PAYMENT_INTENT_RETRIEVAL = "paymentIntentRetrieval";
    public static final String EVENT_PAYMENT_INTENT_CREATION = "paymentIntentCreation";

    //JSON keys
    public static final String ERROR ="error";
    public static final String CODE="code";
    public static final String BATTERY_LEVEL = "batteryLevel";
    public static final String DEVICE_TYPE = "deviceType";
    public static final String SERIAL_NUMBER = "serialNumber";
    public static final String DEVICE_SOFTWARE_VERSION = "deviceSoftwareVersion";
    public static final String STATUS = "status";
    public static final String EVENT = "event";
    public static final String INFO = "info";
    public static final String PAYMENT_INTENT ="paymentIntent";
    public static final String AMOUNT = "amount";
    public static final String CURRENCY =  "currency";
    public static final String APPLICATION_FEE_AMOUNT = "applicationFeeAmount";
    public static final String TEXT="text";
    public static final String STRIPE_ID = "stripeId";
    public static final String CREATED = "created";
    public static final String METADATA ="metadata";
    public static final String INTENT="intent";

    //Put constants

    /*
    @{
        @"DeviceTypeChipper2X": @(SCPDeviceTypeChipper2X),

        @"DiscoveryMethodBluetoothScan": @(SCPDiscoveryMethodBluetoothScan),
        @"DiscoveryMethodBluetoothProximity": @(SCPDiscoveryMethodBluetoothProximity),

        @"PaymentIntentStatusRequiresPaymentMethod": @(SCPPaymentIntentStatusRequiresPaymentMethod),
        @"PaymentIntentStatusRequiresConfirmation": @(SCPPaymentIntentStatusRequiresConfirmation),
        @"PaymentIntentStatusRequiresCapture": @(SCPPaymentIntentStatusRequiresCapture),
        @"PaymentIntentStatusCanceled": @(SCPPaymentIntentStatusCanceled),
        @"PaymentIntentStatusSucceeded": @(SCPPaymentIntentStatusSucceeded),

        @"ReaderEventCardInserted": @(SCPReaderEventCardInserted),
        @"ReaderEventCardRemoved": @(SCPReaderEventCardRemoved),

        @"PaymentStatusNotReady": @(SCPPaymentStatusNotReady),
        @"PaymentStatusReady": @(SCPPaymentStatusReady),
        @"PaymentStatusWaitingForInput": @(SCPPaymentStatusWaitingForInput),
        @"PaymentStatusProcessing": @(SCPPaymentStatusProcessing),

        @"ConnectionStatusNotConnected": @(SCPConnectionStatusNotConnected),
        @"ConnectionStatusConnected": @(SCPConnectionStatusConnected),
        @"ConnectionStatusConnecting": @(SCPConnectionStatusConnecting),
    };


    */

    static{
        constants.put("DeviceTypeChipper2X", DeviceType.CHIPPER_2X.ordinal());
        constants.put("DiscoveryMethodBluetoothScan",0);               //Not applicable for Android SDK
        constants.put("DiscoveryMethodBluetoothProximity",0);          //Not applicable for Android SDK
        constants.put("PaymentIntentStatusRequiresPaymentMethod", PaymentIntent.PaymentIntentStatus.REQUIRES_PAYMENT_METHOD.ordinal());
        constants.put("PaymentIntentStatusRequiresConfirmation", PaymentIntent.PaymentIntentStatus.REQUIRES_CONFIRMATION.ordinal());
        constants.put("PaymentIntentStatusRequiresCapture", PaymentIntent.PaymentIntentStatus.REQUIRES_CAPTURE.ordinal());
        constants.put("PaymentIntentStatusCanceled", PaymentIntent.PaymentIntentStatus.CANCELED.ordinal());
        constants.put("PaymentIntentStatusSucceeded", 4);  //Value not present in android
        constants.put("ReaderEventCardInserted", ReaderEvent.CARD_INSERTED.ordinal());
        constants.put("ReaderEventCardRemoved",ReaderEvent.CARD_REMOVED.ordinal());
        constants.put("PaymentStatusNotReady", PaymentStatus.NOT_READY.ordinal());
        constants.put("PaymentStatusReady",PaymentStatus.READY.ordinal());
        constants.put("PaymentStatusWaitingForInput",PaymentStatus.WAITING_FOR_INPUT.ordinal());
        constants.put("PaymentStatusProcessing",PaymentStatus.PROCESSING.ordinal());
        constants.put("ConnectionStatusNotConnected", ConnectionStatus.NOT_CONNECTED.ordinal());
        constants.put("ConnectionStatusConnected",ConnectionStatus.CONNECTED.ordinal());
        constants.put("ConnectionStatusConnecting",ConnectionStatus.CONNECTING.ordinal());
    }
}
