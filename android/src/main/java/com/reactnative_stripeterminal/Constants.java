package com.reactnative_stripeterminal;

import com.stripe.stripeterminal.model.external.ConnectionStatus;
import com.stripe.stripeterminal.model.external.DeviceType;
import com.stripe.stripeterminal.model.external.PaymentIntent;
import com.stripe.stripeterminal.model.external.PaymentIntentStatus;
import com.stripe.stripeterminal.model.external.PaymentStatus;
import com.stripe.stripeterminal.model.external.ReaderEvent;

import java.util.HashMap;

public class Constants {
    public static final HashMap<String,Object> constants = new HashMap<String,Object>();

    //Plugin Events
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
    public static final String EVENT_PROCESS_PAYMENT = "paymentProcess";
    public static final String EVENT_PAYMENT_INTENT_CANCEL = "paymentIntentCancel";
    public static final String EVENT_READER_CONNECTION = "readerConnection";
    public static final String EVENT_PAYMENT_METHOD_COLLECTION = "paymentMethodCollection";
    public static final String EVENT_READER_DISCONNECTION_COMPLETION = "readerDisconnectCompletion";
    public static final String EVENT_CONNECTED_READER = "connectedReader";
    public static final String EVENT_ABORT_DISCOVER_READER_COMPLETION = "abortDiscoverReadersCompletion";
    public static final String EVENT_PAYMENT_STATUS = "paymentStatus";
    public static final String EVENT_CONNECTION_STATUS = "connectionStatus";
    public static final String EVENT_UPDATE_CHECK = "updateCheck";
    public static final String EVENT_READER_SOFTWARE_UPDATE_PROGRESS = "readerSoftwareUpdateProgress";
    public static final String EVENT_UPDATE_INSTALL = "updateInstall";
    public static final String EVENT_ABORT_INSTALL_COMPLETION = "abortInstallUpdateCompletion";
    public static final String EVENT_ABORT_CREATE_PAYMENT_COMPLETION = "abortCreatePaymentCompletion";

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
    public static final String DECLINE_CODE = "declineCode";
    public static final String ESTIMATED_UPDATE_TIME = "estimatedUpdateTime";
    public static final String ON_BEHALF_OF = "onBehalfOf";
    public static final String TRANSFER_DATA_DESTINATION = "transferDataDestination";
    public static final String TRANSFER_GROUP = "transferGroup";
    public static final String CUSTOMER = "customer";
    public static final String DESCRIPTION = "description";
    public static final String STATEMENT_DESCRIPTOR = "statementDescriptor";
    public static final String RECEIPT_EMAIL = "receiptEmail";
    public static final String UPDATE ="update";

    //Plugin Constants
    static{
        constants.put("DeviceTypeChipper2X", DeviceType.CHIPPER_2X.ordinal());
        constants.put("DiscoveryMethodBluetoothScan",0);               //Not applicable for Android SDK
        constants.put("DiscoveryMethodBluetoothProximity",0);          //Not applicable for Android SDK
        constants.put("PaymentIntentStatusRequiresPaymentMethod", PaymentIntentStatus.REQUIRES_PAYMENT_METHOD.ordinal());
        constants.put("PaymentIntentStatusRequiresConfirmation", PaymentIntentStatus.REQUIRES_CONFIRMATION.ordinal());
        constants.put("PaymentIntentStatusRequiresCapture", PaymentIntentStatus.REQUIRES_CAPTURE.ordinal());
        constants.put("PaymentIntentStatusCanceled", PaymentIntentStatus.CANCELED.ordinal());
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
