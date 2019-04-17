import { NativeModules, NativeEventEmitter } from 'react-native';
import { useStripeTerminalState,
         useStripeTerminalConnectReader,
         useStripeTerminalCreatePayment } from './hooks';

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

    this._createListeners([
      'logEvent',
      'readersDiscovered',
      'didBeginWaitingForReaderInput',
      'didRequestReaderInputPrompt',
      'didReportReaderEvent',
      'didChangePaymentStatus',
      'didChangeConnectionStatus',
      'didDisconnectUnexpectedlyFromReader'
    ])
  }

  _createListeners(keys) {
    for (const k in keys) {
      this[`add${k[0].toUpperCase() + k.substring(1)}Listener`] = \
        (listener) => this.listener.addListener(k, listener);
    }
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

  initialize({ fetchConnectionToken }) {
    this._fetchConnectionToken = fetchConnectionToken;
    RNStripeTerminal.initialize();
  }

  discoverReaders(deviceType, method) {
    RNStripeTerminal.discoverReaders(deviceType, method);
  }

  checkForReaderSoftwareUpdate() {
    RNStripeTerminal.checkForReaderSoftwareUpdate();
  }

  connectReader(serialNumber) {
    return this._wrapPromiseReturn('readerConnection', () => {
      RNStripeTerminal.connectReader(serialNumber);
    });
  }

  getConnectedReader() {
    return this._wrapPromiseReturn('connectedReader', () => {
      RNStripeTerminal.getConnectedReader();
    }).then(data => data.serialNumber ? data : null);
  }

  createPayment(options) {
    return this._wrapPromiseReturn('paymentCreation', () => {
      RNStripeTerminal.createPayment(options);
    }, 'intent');
  }

  abortCreatePayment() {
    RNStripeTerminal.abortCreatePayment();
  }
}

export default const StripeTerminal_ = new StripeTerminal();
export const useStripeTerminalState = useStripeTerminalState.bind(StripeTerminal_);
export const useStripeTerminalConnectReader = useStripeTerminalConnectReader.bind(StripeTerminal_);
export const useStripeTerminalCreatePayment = useStripeTerminalCreatePayment.bind(StripeTerminal_);
