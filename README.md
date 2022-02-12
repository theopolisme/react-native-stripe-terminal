# react-native-stripe-terminal

React Native wrapper for the [Stripe Terminal](https://stripe.com/docs/terminal) SDK version 2. (iOS & Android compatible!)

## Getting started

First, follow all Stripe instructions under ["Install the iOS SDK"](https://stripe.com/docs/terminal/sdk/ios#install) and/or ["Install the Android SDK"](https://stripe.com/docs/terminal/sdk/android#install) (depending on your platform). Then:

`$ npm install git+https://github.com/theopolisme/react-native-stripe-terminal.git --save`

### Mostly automatic installation

`$ react-native link react-native-stripe-terminal`

### Manual installation

#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-stripe-terminal` and add `RNStripeTerminal.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNStripeTerminal.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)

#### Android

1. Open up `android/app/src/main/java/[...]/MainApplication.java`

- Add `import com.reactlibrary.RNStripeTerminalPackage;` to the imports at the top of the file
- Add `new RNStripeTerminalPackage()` to the list returned by the `getPackages()` method

2. Append the following lines to `android/settings.gradle`:
   ```
   include ':react-native-stripe-terminal'
   project(':react-native-stripe-terminal').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-stripe-terminal/android')
   ```
3. Insert the following line inside the dependencies block in `android/app/build.gradle`:
   ```
     compile project(':react-native-stripe-terminal')
   ```

## Usage

The `StripeTerminal` object is a singleton. You must first call `StripeTerminal.initialize` and provide a function to fetch the connection token (see [Stripe docs](https://stripe.com/docs/terminal/ios#connection-token)).

You must also have a method of creating and supplying a [Terminal Location ID](https://stripe.com/docs/api/terminal/locations).

### Basic usage

```javascript
import StripeTerminal, {
  readerUpdateTypes, // contains the reader firmware update types
  simulatedCardTypes, // contains the simulated card types
} from "react-native-stripe-terminal";

// First, initialize the SDK
StripeTerminal.initialize({
  fetchConnectionToken: () => {
    return fetch("https://your.endpoint/terminal", { method: "POST" })
      .then((resp) => resp.json())
      .then((data) => data.secret);
  },
});

// Get a terminal location ID fromyour backend/Stripe API. This is required to establish a connection
// to the reader.
const locationId = "tml_*********";

// Add a listener to handle when readers are discovered.
// You could display the readers to the user to select, or just
// auto-connect to the first available reader.
const discoverListener = StripeTerminal.addReadersDiscoveredListener(
  (readers) => {
    if (readers.length) {
      StripeTerminal.connectReader(readers[0].serialNumber, locationId).then(
        () => {
          // reader is connected
          // now safe to call `StripeTerminal.createPaymentIntent`
        }
      );
    }
  }
);

// When you're ready, scan for readers
StripeTerminal.discoverReaders(
  StripeTerminal.DiscoveryMethodBluetoothProximity,
  0 // Use 1 for "simulated" mode when running in an emulator
);

// After a reader is connected, create a payment intent.
//
// Note: In `react-native-stripe-terminal`, `createPayment`
// abstracts `createPaymentIntent`, collectPaymentMethod`, and
// `confirmPaymentIntent` into a single method. If any of them fail,
// the Promise will be rejected. A resolved promise means that the
// payment was authorized & posted to Stripe and awaits capture.
StripeTerminal.createPayment({ amount: 1200, currency: "usd" })
  .then((intent) => {
    console.log("Payment intent created", intent);
  })
  .catch((error) => {
    console.log(error);
  });

// simulator config utils(only on android)
StripeTerminal.getSimulatorConfiguration().then((config) => {
  console.log("simulator config", config);
});

StripeTerminal.setSimulatorConfiguration(
  readerUpdateTypes.AVAILABLE, // update type(optional - pass null)
  "4000000000000069", // test card number(optional - pass null)
  simulatedCardTypes.AMEX // simulated card type(optional - pass null)
)
  .then((config) => {
    console.log("new simulator config", config);
  })
  .catch((err) => {
    console.log(err);
  });

// reader firmware update utils
StripeTerminal.installUpdate()
  .then((u) => {
    if (u && Object.keys(u).length > 0) {
      console.log("Update installed", u);
    } else {
      console.log("No updates found");
    }
  })
  .catch((e) => {
    console.log("Update not installed", e);
  });

StripeTerminal.abortInstallUpdate()
  .then((u) => {
    if (u && Object.keys(u).length > 0) {
      console.log("Update aborted", u);
    } else {
      console.log("No updates found");
    }
  })
  .catch((e) => {
    console.log("Update not aborted", e);
  });

// You can use the following listeners to update your interface with
// instructions for the user.

// This firing without error does not mean the SDK is not still discovering. Just that it found readers.
// The SDK must be actively discovering in order to connect.
const discoverCompleteListener =
  StripeTerminal.addAbortDiscoverReadersCompletionListener(({ error }) => {
    console.log("AbortDiscoverReadersCompletionListener");
    if (error) {
      this.setState({
        displayText: "Discovery completed with error: " + error,
      });
    }
  });

// Handle changes in reader connection status
const connectionStatusListener =
  StripeTerminal.addDidChangeConnectionStatusListener((event) => {
    // Can check event.status against constants like:
    if (event.status === StripeTerminal.ConnectionStatusConnecting) {
      this.setState({ displayText: "Connecting..." });
    }
    if (event.status === StripeTerminal.ConnectionStatusConnected) {
      this.setState({ displayText: "Connected successfully" });
    }
  });

// Handle unexpected disconnects
const disconnectListener =
  StripeTerminal.addDidReportUnexpectedReaderDisconnectListener((reader) => {
    this.setState({
      displayText:
        "Unexpectedly disconnected from reader " + reader.serialNumber,
    });
  });

// Pass StripeTerminal logs to the Javascript console, if needed
const logListener = StripeTerminal.addLogListener((log) => {
  console.log("[StripeTerminal] -- " + log);
});

const inputListener = StripeTerminal.addDidRequestReaderInputListener(
  (text) => {
    // `text` is a prompt like "Retry Card".
    this.setState({ displayText: text });
  }
);

// update listeners(refer https://stripe.com/docs/terminal/payments/connect-reader?terminal-sdk-platform=android&reader-type=bluetooth#update-reader)
// trigered when there are optional updates available
const updateListener = StripeTerminalModule.addDidReportAvailableUpdateListener(
  (data) => {
    console.log("updateListener", data);
  }
);

// triggered when installing update(this is the first step for required updates)
const startInstallingUpdateListener =
  StripeTerminalModule.addDidStartInstallingUpdateListener((data) => {
    console.log("didStartInstallingUpdateListener", data);
  });

// reports update progress
const didReportReaderSoftwareUpdateProgressListener =
  StripeTerminalModule.addDidReportReaderSoftwareUpdateProgressListener(
    (data) => {
      console.log("didReportReaderSoftwareUpdateProgress", data);
    }
  );

// triggered after the update is installed or aborted
const finishInstallingUpdateListener =
  StripeTerminalModule.addDidFinishInstallingUpdateListener((data) => {
    console.log("didFinishInstallingUpdateListener", data);
  });

// Make sure you remove the listeners when you're done
// (e.g. in componentWillUnmount).
discoverListener.remove();
connectionStatusListener.remove();
disconnectListener.remove();
logListener.remove();
inputListener.remove();
updateListener.remove();
startInstallingUpdateListener.remove();
didReportReaderSoftwareUpdateProgressListener.remove();
finishInstallingUpdateListener.remove();
```

### Hooks usage

If you're running React Native ^0.59 / React ^16.8.0, you can use [Hooks](https://reactjs.org/docs/hooks-intro.html) to seamlessly integrate Stripe Terminal into your React Native application.

```javascript
import StripeTerminal, {
  useStripeTerminal,
  useStripeTerminalCreatePayment,
} from "react-native-stripe-terminal";

// Somewhere early in your application...
StripeTerminal.initialize({
  fetchConnectionToken: () => {
    return fetch("https://your.endpoint/terminal", { method: "POST" })
      .then((resp) => resp.json())
      .then((data) => data.secret);
  },
});

// Then, inside your components...
function PaymentScreen() {
  const {
    connectionStatus,
    connectedReader,
    paymentStatus,
    cardInserted,
    readerInputOptions,
    readerInputPrompt,
  } = useStripeTerminalState();
}

// And when you're finally read to *collect* a payment...
function CollectPaymentScreen() {
  const {
    connectionStatus,
    connectedReader,
    paymentStatus,
    cardInserted,
    readerInputOptions,
    readerInputPrompt,
  } = useStripeTerminalCreatePayment({
    amount: 123,
    description: "Test payment",
    onSuccess: (result) => {
      Alert.alert("Payment received", JSON.stringify(result));
    },
    onFailure: (err) => {
      Alert.alert("Failed to create payment", JSON.stringify(err));
    },
  });
}
```

## Acknowledgements

- [capacitor-stripe-terminal](https://github.com/eventOneHQ/capacitor-stripe-terminal/tree/master)
