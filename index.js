import { NativeModules, NativeEventEmitter } from 'react-native';
import createHooks from './hooks';
import createConnectionService from './connectionService';

const { RNStripeTerminal } = NativeModules;

class StripeTerminal {
  // Device types
  DeviceTypeChipper2X = RNStripeTerminal.DeviceTypeChipper2X;
  DeviceTypeReaderSimulator = RNStripeTerminal.DeviceTypeReaderSimulator;

  // Discovery methods
  DiscoveryMethodBluetoothScan = RNStripeTerminal.DiscoveryMethodBluetoothScan;
  DiscoveryMethodBluetoothProximity =
    RNStripeTerminal.DiscoveryMethodBluetoothProximity;

  // Payment intent statuses
  PaymentIntentStatusRequiresSource =
    RNStripeTerminal.PaymentIntentStatusRequiresSource;
  PaymentIntentStatusRequiresConfirmation =
    RNStripeTerminal.PaymentIntentStatusRequiresConfirmation;
  PaymentIntentStatusRequiresCapture =
    RNStripeTerminal.PaymentIntentStatusRequiresCapture;
  PaymentIntentStatusCanceled = RNStripeTerminal.PaymentIntentStatusCanceled;
  PaymentIntentStatusSucceeded = RNStripeTerminal.PaymentIntentStatusSucceeded;

  // Reader events
  ReaderEventCardInserted = RNStripeTerminal.ReaderEventCardInserted;
  ReaderEventCardRemoved = RNStripeTerminal.ReaderEventCardRemoved;

  // Payment status
  PaymentStatusNotReady = RNStripeTerminal.PaymentStatusNotReady;
  PaymentStatusReady = RNStripeTerminal.PaymentStatusReady;
  PaymentStatusCollectingPaymentMethod =
    RNStripeTerminal.PaymentStatusCollectingPaymentMethod;
  PaymentStatusConfirmingPaymentIntent =
    RNStripeTerminal.PaymentStatusConfirmingPaymentIntent;

  // Connection status
  ConnectionStatusNotConnected = RNStripeTerminal.ConnectionStatusNotConnected;
  ConnectionStatusConnected = RNStripeTerminal.ConnectionStatusConnected;
  ConnectionStatusBusy = RNStripeTerminal.ConnectionStatusBusy;

  // Fetch connection token. Overwritten in call to initialize
  _fetchConnectionToken = () =>
    Promise.reject('You must initialize RNStripeTerminal first.');

  constructor() {
    this.listener = new NativeEventEmitter(RNStripeTerminal);

    this.listener.addListener('requestConnectionToken', () => {
      this._fetchConnectionToken()
        .then(token => {
          if (token) {
            RNStripeTerminal.setConnectionToken(token, null);
          } else {
            throw new Error('User-supplied `fetchConnectionToken` resolved successfully, but no token was returned.');
          }
        })
        .catch(err => RNStripeTerminal.setConnectionToken(null, err || 'Error in user-supplied `fetchConnectionToken`.'));
    });

    this._createListeners([
      'log',
      'readersDiscovered',
      'didBeginWaitingForReaderInput',
      'didRequestReaderInputPrompt',
      'didReportReaderEvent',
      'didChangePaymentStatus',
      'didChangeConnectionStatus',
      'didDisconnectUnexpectedlyFromReader',
    ]);
  }

  _createListeners(keys) {
    keys.forEach(k => {
      this[`add${k[0].toUpperCase() + k.substring(1)}Listener`] = listener =>
        this.listener.addListener(k, listener);
    });
  }

  _wrapPromiseReturn(event, call, key) {
    return new Promise((resolve, reject) => {
      const subscription = this.listener.addListener(event, data => {
        if (data && data.error) {
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
    return this._wrapPromiseReturn('readerDiscoveryCompletion', () => {
      RNStripeTerminal.discoverReaders(deviceType, method);
    });
  }

  checkForReaderSoftwareUpdate() {
    RNStripeTerminal.checkForReaderSoftwareUpdate();
  }

  connectReader(serialNumber) {
    return this._wrapPromiseReturn('readerConnection', () => {
      RNStripeTerminal.connectReader(serialNumber);
    });
  }

  disconnectReader() {
    return this._wrapPromiseReturn('readerDisconnectCompletion', () => {
      RNStripeTerminal.disconnectReader();
    });
  }

  getConnectedReader() {
    return this._wrapPromiseReturn('connectedReader', () => {
      RNStripeTerminal.getConnectedReader();
    }).then(data => (data.serialNumber ? data : null));
  }

  getConnectionStatus() {
    return this._wrapPromiseReturn('connectionStatus', () => {
      RNStripeTerminal.getConnectionStatus();
    });
  }

  getPaymentStatus() {
    return this._wrapPromiseReturn('paymentStatus', () => {
      RNStripeTerminal.getPaymentStatus();
    });
  }

  getLastReaderEvent() {
    return this._wrapPromiseReturn('lastReaderEvent', () => {
      RNStripeTerminal.getLastReaderEvent();
    });
  }

  createPayment(options) {
    return this._wrapPromiseReturn(
      'paymentCreation',
      () => {
        RNStripeTerminal.createPayment(options);
      },
      'intent',
    );
  }

  abortCreatePayment() {
    return this._wrapPromiseReturn('abortCreatePaymentCompletion', () => {
      RNStripeTerminal.abortCreatePayment();
    });
  }

  abortDiscoverReaders() {
    return this._wrapPromiseReturn('abortDiscoverReadersCompletion', () => {
      RNStripeTerminal.abortDiscoverReaders();
    });
  }

  startService(options) {
    if (typeof options === 'string') {
      options = { policy: options };
    }

    if (this._currentService) {
      return Promise.reject(
        'A service is already running. You must stop it using `stopService` before starting a new service.',
      );
    }

    this._currentService = createConnectionService(this, options);
    this._currentService.start();
    return this._currentService;
  }

  stopService() {
    if (!this._currentService) {
      return Promise.resolve();
    }

    return this._currentService.stop().then(() => {
      this._currentService = null;
    });
  }
}

const StripeTerminal_ = new StripeTerminal();
export default StripeTerminal_;

export const {
  useStripeTerminalState,
  useStripeTerminalCreatePayment,
  useStripeTerminalConnectionManager,
} = createHooks(StripeTerminal_);
