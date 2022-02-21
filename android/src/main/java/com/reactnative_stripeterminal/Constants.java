package com.reactnative_stripeterminal;

import com.stripe.stripeterminal.external.models.ConnectionStatus;
import com.stripe.stripeterminal.external.models.DeviceType;
import com.stripe.stripeterminal.external.models.DiscoveryMethod;
import com.stripe.stripeterminal.external.models.PaymentIntent;
import com.stripe.stripeterminal.external.models.PaymentIntentStatus;
import com.stripe.stripeterminal.external.models.PaymentStatus;
import com.stripe.stripeterminal.external.models.ReaderEvent;
import com.stripe.stripeterminal.external.models.SimulateReaderUpdate;
import com.stripe.stripeterminal.external.models.SimulatedCardType;

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
    public static final String EVENT_DID_REPORT_AVAILABLE_UPDATE = "didReportAvailableUpdate";
    public static final String EVENT_DID_START_INSTALLING_UPDATE = "didStartInstallingUpdate";
    public static final String EVENT_DID_REPORT_UPDATE_PROGRESS = "didReportReaderSoftwareUpdateProgress";
    public static final String EVENT_DID_FINISH_INSTALLING_UPDATE = "didFinishInstallingUpdate";
    public static final String EVENT_ABORT_INSTALL_COMPLETION = "abortInstallUpdateCompletion";
    public static final String EVENT_ABORT_CREATE_PAYMENT_COMPLETION = "abortCreatePaymentCompletion";

    //JSON keys
    public static final String ERROR ="error";
    public static final String API_ERROR = "apiError";
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
    public static final String UPDATE_TYPE = "updateType";
    public static final String CARD = "card";
    public static final String REQUIRED_AT = "requiredAt";
    public static final String AVAILABLE_UPDATE = "availableUpdate";

    //Plugin Constants
    static{
        constants.put("DeviceTypeChipper2X", DeviceType.CHIPPER_2X.ordinal());
        constants.put("DiscoveryMethodBluetoothScan", DiscoveryMethod.BLUETOOTH_SCAN.ordinal());
        constants.put("DiscoveryMethodBluetoothProximity", DiscoveryMethod.BLUETOOTH_SCAN.ordinal()); //Not applicable for Android SDK
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

        // Simulator Reader Update
        constants.put("ReaderUpdateAvailable", SimulateReaderUpdate.UPDATE_AVAILABLE.ordinal());
        constants.put("ReaderUpdateRequired", SimulateReaderUpdate.REQUIRED.ordinal());
        constants.put("ReaderUpdateNone", SimulateReaderUpdate.NONE.ordinal());
        constants.put("ReaderUpdateRandom", SimulateReaderUpdate.RANDOM.ordinal());

        // Simulated Card Type
        constants.put("CardMastercard", SimulatedCardType.MASTERCARD.ordinal());
        constants.put("CardMastercardDebit", SimulatedCardType.MASTERCARD_DEBIT.ordinal());
        constants.put("CardMastercardPrepaid", SimulatedCardType.MASTERCARD_PREPAID.ordinal());
        constants.put("CardAmex", SimulatedCardType.AMEX.ordinal());
        constants.put("CardAmex2", SimulatedCardType.AMEX_2.ordinal());
        constants.put("CardVisa", SimulatedCardType.VISA.ordinal());
        constants.put("CardVisaDebit", SimulatedCardType.VISA_DEBIT.ordinal());
        constants.put("CardDiscover", SimulatedCardType.DISCOVER.ordinal());
        constants.put("CardDiscover2", SimulatedCardType.DISCOVER_2.ordinal());
        constants.put("CardDiners", SimulatedCardType.DINERS.ordinal());
        constants.put("CardDiners14", SimulatedCardType.DINERS_14_DIGITS.ordinal());
        constants.put("CardJcb", SimulatedCardType.JCB.ordinal());
        constants.put("CardUnion", SimulatedCardType.UNION_PAY.ordinal());
        constants.put("CardInterac", SimulatedCardType.INTERAC.ordinal());
        constants.put("CardDeclined", SimulatedCardType.CHARGE_DECLINED.ordinal());
        constants.put("CardDeclinedInsufficientFunds", SimulatedCardType.CHARGE_DECLINED_INSUFFICIENT_FUNDS.ordinal());
        constants.put("CardDeclinedExpired", SimulatedCardType.CHARGE_DECLINED_EXPIRED_CARD.ordinal());
        constants.put("CardDeclinedLost", SimulatedCardType.CHARGE_DECLINED_LOST_CARD.ordinal());
        constants.put("CardDeclinedStolen", SimulatedCardType.CHARGE_DECLINED_STOLEN_CARD.ordinal());
        constants.put("CardDeclinedProcessingError", SimulatedCardType.CHARGE_DECLINED_PROCESSING_ERROR.ordinal());
    }
}
