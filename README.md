# react-native-stripe-terminal

React Native wrapper for the [Stripe Terminal](https://stripe.com/docs/terminal/ios) SDK. (iOS-only, for now.)

## Getting started

First, follow all Stripe instructions under ["Set up the iOS SDK"](https://stripe.com/docs/terminal/ios#install). Then:

`$ npm install git+https://github.com/theopolisme/react-native-stripe-terminal.git --save`

### Mostly automatic installation

`$ react-native link react-native-stripe-terminal`

### Manual installation

#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-stripe-terminal` and add `RNStripeTerminal.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNStripeTerminal.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)

## Hooks usage

If you're running React Native ^0.59 / React ^16.8.0, you can use [Hooks](https://reactjs.org/docs/hooks-intro.html) to seamlessly integrate Stripe Terminal into your React Native application. 

```javascript
import StripeTerminal, { useStripeTerminal, useStripeTerminalCreatePayment } from 'react-native-stripe-terminal';

// Somewhere early in your application...
StripeTerminal.initialize({
  fetchConnectionToken: () => {
    return fetch('https://your.endpoint/terminal', { method: 'POST' })
      .then(resp => resp.json())
      .then(data => data.secret);
  }
});

// Then, inside your components...
function PaymentScreen() {
  const {
    
  }
}


```

## Basic usage

The `StripeTerminal` object is a singleton. You must first call `StripeTerminal.initialize` and provide a function to fetch the connection token (see [Stripe docs](https://stripe.com/docs/terminal/ios#connection-token)).

```javascript
import StripeTerminal from 'react-native-stripe-terminal';

// First, initialize the SDK
StripeTerminal.initialize({
  fetchConnectionToken: () => {
    return fetch('https://your.endpoint/terminal', { method: 'POST' })
      .then(resp => resp.json())
      .then(data => data.secret);
  }
});

// Add a listener to handle when readers are discovered.
// You could display the readers to the user to select, or just
// auto-connect to the first available reader.
StripeTerminal.addReadersDiscoveredListener(readers => {
  if (readers.length) {
    StripeTerminal.connectReader(readers[0].serialNumber)
      .then(() => {
        // reader is connected
        // now safe to call `StripeTerminal.createPaymentIntent`
      });
  }
});

// When you're ready, scan for readers
StripeTerminal.discoverReaders(
  StripeTerminal.DeviceTypeChipper2X,
  StripeTerminal.DiscoveryMethodBluetoothProximity);

// After.a reader is connected, create a payment intent.
// 
// Note: In `react-native-stripe-terminal`, `createPaymentIntent`
// abstracts `createPaymentIntent`, collectPaymentMethod`, and
// `confirmPaymentIntent` into a single method. If any of them fail,
// the Promise will be rejected. A resolved promise means that the
// payment was authorized & posted to Stripe and awaits capture.
StripeTerminal.createPaymentIntent({ amount: 1200, currency: "usd" })
  .then(intent => {
    console.log('Payment intent created', intent);
  })
  .catch(error => {
    console.log(error);
  });

// You can use the following listeners to update your interface with
// instructions for the user.
const waitingListener = StripeTerminal.addDidBeginWaitingForReaderInputListener(text => {
  // `text` is a string of instructions, like "Swipe / Tap / Dip".
  this.setState({ displayText: text });
});
const inputListener = StripeTerminal.addDidRequestReaderInputPrompt(text => {
  // `text` is a prompt like "Retry Card".
  this.setState({ displayText: text });
});

// Make sure you remove the listeners when you're done
// (e.g. in componentWillUnmount).
waitingListener.remove();
inputListener.remove();

```
  