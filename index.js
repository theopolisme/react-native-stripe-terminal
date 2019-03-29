import { NativeModules, NativeEventEmitter } from 'react-native';

const { RNStripeTerminal } = NativeModules;

class StripeTerminal {
  constructor({ fetchConnectionToken }) {
    this.listener = new NativeEventEmitter(RNStripeTerminal);
    this.listener.on('requestConnectionToken', () => {
      fetchConnectionToken()
        .then(token => RNStripeTerminal.setConnectionToken(token, null))
        .catch(err => RNStripeTerminal.setConnectionToken(null, err));
    });

    RNStripeTerminal.initialize();
  }
}

export default StripeTerminal;
