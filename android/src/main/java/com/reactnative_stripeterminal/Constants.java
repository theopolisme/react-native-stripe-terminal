package com.reactnative_stripeterminal;

import android.net.wifi.aware.DiscoverySession;

import com.stripe.stripeterminal.ConnectionStatus;
import com.stripe.stripeterminal.DeviceType;
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

    //JSON keys
    public static final String ERROR="error";
    public static final String BATTERY_LEVEL = "batteryLevel";
    public static final String DEVICE_TYPE = "deviceType";
    public static final String SERIAL_NUMBER = "serialNumber";
    public static final String DEVICE_SOFTWARE_VERSION = "deviceSoftwareVersion";
    public static final String STATUS = "status";
    public static final String EVENT = "event";
    public static final String INFO = "info";

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
        constants.put("PaymentIntentStatusRequiresPaymentMethod","");
        constants.put("PaymentIntentStatusRequiresConfirmation","");
        constants.put("PaymentIntentStatusRequiresCapture","");
        constants.put("PaymentIntentStatusCanceled","");
        constants.put("PaymentIntentStatusSucceeded","");
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
