import { NativeModules, NativeEventEmitter, Platform } from 'react-native';
import createHooks from './hooks';
import createConnectionService from './connectionService';
import * as types from './types';

const RNStripeTerminal: any = NativeModules.RNStripeTerminal;

export class StripeTerminal {
  // Device types
  DeviceTypeChipper2X = RNStripeTerminal.DeviceTypeChipper2X;

  // Discovery methods
  DiscoveryMethodBluetoothScan = RNStripeTerminal.DiscoveryMethodBluetoothScan;
  DiscoveryMethodBluetoothProximity = RNStripeTerminal.DiscoveryMethodBluetoothProximity;

  // Payment intent statuses
  PaymentIntentStatusRequiresPaymentMethod =
    RNStripeTerminal.PaymentIntentStatusRequiresPaymentMethod;
  PaymentIntentStatusRequiresConfirmation =
    RNStripeTerminal.PaymentIntentStatusRequiresConfirmation;
  PaymentIntentStatusRequiresCapture = RNStripeTerminal.PaymentIntentStatusRequiresCapture;
  PaymentIntentStatusCanceled = RNStripeTerminal.PaymentIntentStatusCanceled;
  PaymentIntentStatusSucceeded = RNStripeTerminal.PaymentIntentStatusSucceeded;

  // Reader events
  ReaderEventCardInserted = RNStripeTerminal.ReaderEventCardInserted;
  ReaderEventCardRemoved = RNStripeTerminal.ReaderEventCardRemoved;

  // Payment status
  PaymentStatusNotReady = RNStripeTerminal.PaymentStatusNotReady;
  PaymentStatusReady = RNStripeTerminal.PaymentStatusReady;
  PaymentStatusWaitingForInput = RNStripeTerminal.PaymentStatusWaitingForInput;
  PaymentStatusProcessing = RNStripeTerminal.PaymentStatusProcessing;

  // Connection status
  ConnectionStatusNotConnected = RNStripeTerminal.ConnectionStatusNotConnected;
  ConnectionStatusConnected = RNStripeTerminal.ConnectionStatusConnected;
  ConnectionStatusConnecting = RNStripeTerminal.ConnectionStatusConnecting;

  listener: NativeEventEmitter;

  addLogListener: any;
  addReadersDiscoveredListener: any;
  addReaderSoftwareUpdateProgressListener: any;
  addDidRequestReaderInputListener: any;
  addDidRequestReaderDisplayMessageListener: any;
  addDidReportReaderEventListener: any;
  addDidReportLowBatteryWarningListener: any;
  addDidChangePaymentStatusListener: any;
  addDidChangeConnectionStatusListener: any;
  addDidReportUnexpectedReaderDisconnectListener: any;

  removeLogListener: any;
  removeReadersDiscoveredListener: any;
  removeReaderSoftwareUpdateProgressListener: any;
  removeDidRequestReaderInputListener: any;
  removeDidRequestReaderDisplayMessageListener: any;
  removeDidReportReaderEventListener: any;
  removeDidReportLowBatteryWarningListener: any;
  removeDidChangePaymentStatusListener: any;
  removeDidChangeConnectionStatusListener: any;
  removeDidReportUnexpectedReaderDisconnectListener: any;

  _currentService: any;

  // Fetch connection token. Overwritten in call to initialize
  _fetchConnectionToken = () => Promise.reject('You must initialize RNStripeTerminal first.');

  constructor() {
    this.listener = new NativeEventEmitter(RNStripeTerminal);

    this.listener.addListener('requestConnectionToken', () => {
      this._fetchConnectionToken()
        .then((token) => {
          if (token) {
            RNStripeTerminal.setConnectionToken(token, null);
          } else {
            throw new Error(
              'User-supplied `fetchConnectionToken` resolved successfully, but no token was returned.',
            );
          }
        })
        .catch((err) =>
          RNStripeTerminal.setConnectionToken(
            null,
            err.message || 'Error in user-supplied `fetchConnectionToken`.',
          ),
        );
    });

    this._createListeners([
      'log',
      'readersDiscovered',
      'readerSoftwareUpdateProgress',
      'didRequestReaderInput',
      'didRequestReaderDisplayMessage',
      'didReportReaderEvent',
      'didReportLowBatteryWarning',
      'didChangePaymentStatus',
      'didChangeConnectionStatus',
      'didReportUnexpectedReaderDisconnect',
    ]);
  }

  _createListeners(keys) {
    keys.forEach((k) => {
      this[`add${k[0].toUpperCase() + k.substring(1)}Listener`] = (listener) =>
        this.listener.addListener(k, listener);
      this[`remove${k[0].toUpperCase() + k.substring(1)}Listener`] = (listener) =>
        this.listener.removeListener(k, listener);
    });
  }

  _wrapPromiseReturn = (event, call, key?: string): Promise<any> => {
    return new Promise((resolve, reject) => {
      const subscription = this.listener.addListener(event, (data) => {
        if (data && data.error) {
          reject(data);
        } else {
          resolve(key ? data[key] : data);
        }
        subscription.remove();
      });

      call();
    });
  };

  initialize = ({ fetchConnectionToken }) => {
    this._fetchConnectionToken = fetchConnectionToken;
    return new Promise((resolve, reject) => {
      if (Platform.OS === 'android') {
        RNStripeTerminal.initialize((status) => {
          if (status.isInitialized === true) {
            resolve();
          } else {
            reject(status.error);
          }
        });
      } else {
        RNStripeTerminal.initialize();
        resolve();
      }
    });
  };

  discoverReaders = (deviceType, method, simulated) =>
    this._wrapPromiseReturn('readerDiscoveryCompletion', () => {
      RNStripeTerminal.discoverReaders(deviceType, method, simulated);
    });

  checkForUpdate = () =>
    this._wrapPromiseReturn(
      'updateCheck',
      () => {
        RNStripeTerminal.checkForUpdate();
      },
      'update',
    );

  installUpdate = () =>
    this._wrapPromiseReturn('updateInstall', () => {
      RNStripeTerminal.installUpdate();
    });

  connectReader = (serialNumber: string) =>
    this._wrapPromiseReturn('readerConnection', () => {
      RNStripeTerminal.connectReader(serialNumber);
    });

  disconnectReader = () =>
    this._wrapPromiseReturn('readerDisconnectCompletion', () => {
      RNStripeTerminal.disconnectReader();
    });

  getConnectedReader = () =>
    this._wrapPromiseReturn('connectedReader', () => {
      RNStripeTerminal.getConnectedReader();
    }).then((data: types.Reader) => (data.serialNumber ? data : null));

  getConnectionStatus = () =>
    this._wrapPromiseReturn('connectionStatus', () => {
      RNStripeTerminal.getConnectionStatus();
    });

  getPaymentStatus = () =>
    this._wrapPromiseReturn('paymentStatus', () => {
      RNStripeTerminal.getPaymentStatus();
    });

  getLastReaderEvent = () =>
    this._wrapPromiseReturn('lastReaderEvent', () => {
      RNStripeTerminal.getLastReaderEvent();
    });

  createPayment = (options: any) =>
    this._wrapPromiseReturn(
      'paymentCreation',
      () => {
        RNStripeTerminal.createPayment(options);
      },
      'intent',
    );

  createPaymentIntent = (options: any) =>
    this._wrapPromiseReturn(
      'paymentIntentCreation',
      () => {
        RNStripeTerminal.createPaymentIntent(options);
      },
      'intent',
    );

  retrievePaymentIntent = (clientSecret: string) =>
    this._wrapPromiseReturn(
      'paymentIntentRetrieval',
      () => {
        RNStripeTerminal.retrievePaymentIntent(clientSecret);
      },
      'intent',
    );

  collectPaymentMethod = () =>
    this._wrapPromiseReturn(
      'paymentMethodCollection',
      () => {
        RNStripeTerminal.collectPaymentMethod();
      },
      'intent',
    );

  processPayment = () =>
    this._wrapPromiseReturn(
      'paymentProcess',
      () => {
        RNStripeTerminal.processPayment();
      },
      'intent',
    );

  cancelPaymentIntent() {
    return this._wrapPromiseReturn(
      'paymentIntentCancel',
      () => {
        RNStripeTerminal.cancelPaymentIntent();
      },
      'intent',
    );
  }

  abortCreatePayment = () =>
    this._wrapPromiseReturn('abortCreatePaymentCompletion', () => {
      RNStripeTerminal.abortCreatePayment();
    });

  abortDiscoverReaders = () =>
    this._wrapPromiseReturn('abortDiscoverReadersCompletion', () => {
      RNStripeTerminal.abortDiscoverReaders();
    });

  abortInstallUpdate = () =>
    this._wrapPromiseReturn('abortInstallUpdateCompletion', () => {
      RNStripeTerminal.abortInstallUpdate();
    });

  startService(options: types.ServiceOptions) {
    // if (typeof options === 'string') {
    //   options = { policy: options };
    // }

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
