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

  constructor() {
    this.listener = new NativeEventEmitter(RNStripeTerminal);
  }

  initialize({ fetchConnectionToken }) {
    this.listener.addListener('requestConnectionToken', () => {
      fetchConnectionToken()
        .then(token => RNStripeTerminal.setConnectionToken(token, null))
        .catch(err => RNStripeTerminal.setConnectionToken(null, err));
    });

    RNStripeTerminal.initialize();
  }

  addReadersDiscoveredListener(listener) {
    return this.listener.addListener('readersDiscovered', listener);
  }

  addDidBeginWaitingForReaderInputListener(listener) {
    return this.listener.addListener('didBeginWaitingForReaderInput', listener);
  }

  addDidRequestReaderInputPrompt(listener) {
    return this.listener.addListener('didRequestReaderInputPrompt', listener);
  }

  discoverReaders(deviceType, method) {
    RNStripeTerminal.discoverReaders(deviceType, method);
  }

  _wrapPromiseReturn(event, call, key) {
    return new Promise((resolve, reject) => {
      const subscription = this.listener.addListener(event, data => {
        if (data.error) {
          reject(data.error);
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
}

const StripeTerminal_ = new StripeTerminal();

export default StripeTerminal_;
