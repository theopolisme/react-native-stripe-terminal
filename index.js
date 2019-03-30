import { NativeModules, NativeEventEmitter } from 'react-native';

const { RNStripeTerminal } = NativeModules;

class StripeTerminal {

  // Device types
  DeviceTypeChipper2X = RNStripeTerminal.DeviceTypeChipper2X;
  DeviceTypeReaderSimulator = RNStripeTerminal.DeviceTypeReaderSimulator;

  // Discovery methods
  DiscoveryMethodBluetoothScan = RNStripeTerminal.DiscoveryMethodBluetoothScan;
  DiscoveryMethodBluetoothProximity = RNStripeTerminal.DiscoveryMethodBluetoothProximity;

  // Payment intent statuses
  PaymentIntentStatusRequiresSource = RNStripeTerminal.PaymentIntentStatusRequiresSource;
  PaymentIntentStatusRequiresConfirmation = RNStripeTerminal.PaymentIntentStatusRequiresConfirmation;
  PaymentIntentStatusRequiresCapture = RNStripeTerminal.PaymentIntentStatusRequiresCapture;
  PaymentIntentStatusCanceled = RNStripeTerminal.PaymentIntentStatusCanceled;
  PaymentIntentStatusSucceeded = RNStripeTerminal.PaymentIntentStatusSucceeded;

  // Reader events
  ReaderEventCardInserted = RNStripeTerminal.ReaderEventCardInserted;
  ReaderEventCardRemoved = RNStripeTerminal.ReaderEventCardRemoved;

  // Payment status
  PaymentStatusNotReady = RNStripeTerminal.PaymentStatusNotReady;
  PaymentStatusReady = RNStripeTerminal.PaymentStatusReady;
  PaymentStatusCollectingPaymentMethod = RNStripeTerminal.PaymentStatusCollectingPaymentMethod;
  PaymentStatusConfirmingPaymentIntent = RNStripeTerminal.PaymentStatusConfirmingPaymentIntent;

  // Connection status
  ConnectionStatusNotConnected = RNStripeTerminal.ConnectionStatusNotConnected;
  ConnectionStatusConnected = RNStripeTerminal.ConnectionStatusConnected;
  ConnectionStatusBusy = RNStripeTerminal.ConnectionStatusBusy;

  // Fetch connection token. Overwritten in call to initialize
  _fetchConnectionToken = () => Promise.reject('You must initialize RNStripeTerminal first.');

  constructor() {
    this.listener = new NativeEventEmitter(RNStripeTerminal);

    this.listener.addListener('requestConnectionToken', () => {
      this._fetchConnectionToken()
        .then(token => RNStripeTerminal.setConnectionToken(token, null))
        .catch(err => RNStripeTerminal.setConnectionToken(null, err));
    });
  }

  initialize({ fetchConnectionToken }) {
    this._fetchConnectionToken = fetchConnectionToken;
    RNStripeTerminal.initialize();
  }

  addReadersDiscoveredListener(listener) {
    return this.listener.addListener('readersDiscovered', listener);
  }

  addDidBeginWaitingForReaderInputListener(listener) {
    return this.listener.addListener('didBeginWaitingForReaderInput', listener);
  }

  addDidRequestReaderInputPromptListener(listener) {
    return this.listener.addListener('didRequestReaderInputPrompt', listener);
  }

  addDidReportReaderEventListener(listener) {
    return this.listener.addListener('didReportReaderEvent', listener);
  }

  addDidChangePaymentStatusListener(listener) {
    return this.listener.addListener('didChangePaymentStatus', listener);
  }

  addDidChangeConnectionStatusListener(listener) {
    return this.listener.addListener('didChangeConnectionStatus', listener);
  }

  addDidDisconnectUnexpectedlyFromReaderListener(listener) {
    return this.listener.addListener('didDisconnectUnexpectedlyFromReader', listener);
  }

  discoverReaders(deviceType, method) {
    RNStripeTerminal.discoverReaders(deviceType, method);
  }

  checkForReaderSoftwareUpdate() {
    RNStripeTerminal.checkForReaderSoftwareUpdate();
  }

  _wrapPromiseReturn(event, call, key) {
    return new Promise((resolve, reject) => {
      const subscription = this.listener.addListener(event, data => {
        if (data.error) {
          reject(data);
        } else {
          resolve(key ? data[key] : data);
        }
        subscription.remove();
      });

      call();
    });
  }

  connectReader(serialNumber) {
    return this._wrapPromiseReturn('readerConnection', () => {
      RNStripeTerminal.connectReader(serialNumber);
    });
  }

  createPaymentIntent(options) {
    return this._wrapPromiseReturn('paymentIntentCreation', () => {
      RNStripeTerminal.createPaymentIntent(options);
    }, 'intent');
  }

  getConnectedReader() {
    return this._wrapPromiseReturn('connectedReader', () => {
      RNStripeTerminal.getConnectedReader();
    }).then(data => data.serialNumber ? data : null);
  }

  abortCreatePaymentIntent() {
    RNStripeTerminal.abortCreatePaymentIntent();
  }
}

const StripeTerminal_ = new StripeTerminal();

export default StripeTerminal_;
