/**DiscoveryMethodBluetoothProximity
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 * @flow
 */

import React, { Component } from "react";
import {
  Platform,
  StyleSheet,
  Text,
  View,
  TouchableOpacity,
  TextInput,
  Switch,
} from "react-native";
import StripeTerminal, {
  readerUpdateTypes,
  simulatedCardTypes,
} from "react-native-stripe-terminal";
import RNAndroidLocationEnabler from "react-native-android-location-enabler";
import { PERMISSIONS, request, RESULTS } from "react-native-permissions";

const instructions = Platform.select({
  ios: "Press Cmd+R to reload,\n" + "Cmd+D or shake for dev menu",
  android:
    "Double tap R on your keyboard to reload,\n" +
    "Shake or press menu button for dev menu",
});

export default class App extends Component {
  constructor(props) {
    super(props);
    this.state = {
      isConnecting: false,
      readerConnected: false,
      completedPayment: "loading...",
      displayText: "Loading...",
      connectedReader: "None",
      isSimulated: false,
      locationId: "tml_*********",
      discoveryMethod: 0, // false = internet, true = bluetooth
      // locationEnabled: true,
    };

    this.BACKEND_URL = "https://your.backend.com";

    this.discover = this.discover.bind(this);
    this.createPayment = this.createPayment.bind(this);

    this.discoverListener = StripeTerminal.addReadersDiscoveredListener(
      (readers) => {
        console.log("readers discovered", readers);
        if (
          readers.length &&
          !this.state.readerConnected &&
          !this.state.isConnecting
        ) {
          this.setState({ isConnecting: true });
          StripeTerminal.connectReader(
            readers[0].serialNumber,
            this.state.locationId
          )
            .then(() => {
              this.setState({
                isConnecting: false,
                connectedReader: readers[0].serialNumber,
                completedPayment: "connected to reader",
              });
            })
            .catch((e) => {
              console.log("failed to connect", e);
              // alert("failed to connect " + JSON.stringify(e));
            });
        }
      }
    );

    // This firing without error does not mean the SDK is not still discovering. Just that it found readers.
    // The SDK must be actively discovering in order to connect.
    this.discoverCompleteListener = StripeTerminal.addAbortDiscoverReadersCompletionListener(
      (data) => {
        console.log("AbortDiscoverReadersCompletionListener");
        if (data.error) {
          this.setState({
            completedPayment: "Discovery completed with error: " + data.error,
          });
        }
      }
    );

    // Handle changes in reader connection status
    this.connectionStatusListener = StripeTerminal.addDidChangeConnectionStatusListener(
      (event) => {
        // Can check event.status against constants like:
        if (event.status === StripeTerminal.ConnectionStatusConnecting) {
          this.setState({ displayText: "Connecting..." });
        }
        if (event.status === StripeTerminal.ConnectionStatusConnected) {
          this.setState({ displayText: "Connected successfully" });
        }
      }
    );

    // Handle unexpected disconnects
    this.disconnectListener = StripeTerminal.addDidReportUnexpectedReaderDisconnectListener(
      (reader) => {
        this.setState({
          displayText:
            "Unexpectedly disconnected from reader " + reader.serialNumber,
        });
      }
    );

    // Pass StripeTerminal logs to the Javascript console, if needed
    this.logListener = StripeTerminal.addLogListener((log) => {
      console.log("[StripeTerminal] -- " + log);
    });

    this.inputListener = StripeTerminal.addDidRequestReaderInputListener(
      (text) => {
        // `text` is a prompt like "Retry Card".
        this.setState({ displayText: text });
      }
    );

    this.updateListener = StripeTerminal.addDidReportAvailableUpdateListener(
      (data) => {
        console.log("updateListener", data);
      }
    );

    this.startInstallingUpdateListener = StripeTerminal.addDidStartInstallingUpdateListener(
      (data) => {
        console.log("didStartInstallingUpdateListener", data);
      }
    );

    this.didReportReaderSoftwareUpdateProgressListener = StripeTerminal.addDidReportReaderSoftwareUpdateProgressListener(
      (data) => {
        console.log("didReportReaderSoftwareUpdateProgress", data);
      }
    );

    this.finishInstallingUpdateListener = StripeTerminal.addDidFinishInstallingUpdateListener(
      (data) => {
        console.log("didFinishInstallingUpdateListener", data);
      }
    );
  }

  askPermission(permission) {
    let permissionObject = {};

    switch (permission) {
      case "location":
        permissionObject = {
          android: PERMISSIONS.ANDROID.ACCESS_FINE_LOCATION,
          ios: PERMISSIONS.IOS.LOCATION_ALWAYS,
        };
        break;
      default:
        permissionObject = {};
    }

    return new Promise((resolve, reject) => {
      request(Platform.select(permissionObject))
        .then((result) => {
          switch (result) {
            case RESULTS.UNAVAILABLE:
              console.log(
                "This feature is not available (on this device / in this context)"
              );
              reject(
                `${permission}: This feature is not available (on this device / in this context)`
              );
              break;
            case RESULTS.DENIED:
              console.log(
                "The permission has not been requested / is denied but request-able"
              );
              reject(
                `${permission}: The permission has not been requested / is denied but request-able`
              );
              break;
            case RESULTS.GRANTED:
              console.log("The permission: " + permission + " is granted");
              resolve(result);
              break;
            case RESULTS.BLOCKED:
              console.log(
                "The permission is denied and not request-able anymore"
              );
              reject(
                `${permission}: The permission is denied and not request-able anymore`
              );
              break;
          }
        })
        .catch((e) => {
          console.warn(e);
          reject(`${permission}: ${e}`);
        });
    });
  }

  async componentDidMount() {
    // for newer sripe terminal SDKs
    // await this.askPermission(
    //   PermissionsAndroid.PERMISSIONS.ACCESS_COARSE_LOCATION
    // );

    this.askPermission("location")
      .then(() => {
        if (Platform.OS === "android") {
          return RNAndroidLocationEnabler.promptForEnableLocationIfNeeded({
            interval: 10000,
            fastInterval: 5000,
          });
        } else {
          return Promise.resolve("Success");
        }
      })
      .then(() => {
        StripeTerminal.initialize({
          fetchConnectionToken: () => {
            console.log("fetching connection token");
            return fetch(this.BACKEND_URL + "/connection_token", {
              method: "POST",
            })
              .then((resp) => resp.json())
              .then((data) => {
                StripeTerminal.getSimulatorConfiguration().then((config) => {
                  console.log("SIMULATED before", config);
                });
                StripeTerminal.setSimulatorConfiguration(
                  readerUpdateTypes.AVAILABLE,
                  "4000000000000069",
                  simulatedCardTypes.AMEX
                )
                  .then((config) => {
                    console.log("SIMULATED", config);
                  })
                  .catch((err) => {
                    console.log(err);
                  });
                StripeTerminal.getSimulatorConfiguration().then((config) => {
                  console.log("SIMULATED after", config);
                });
                console.log("got data fetchConnectionToken", data);
                return data.secret;
              })
              .catch((err) => {
                console.log("fetchConnectionToken error", err);
                // alert("fetchConnectionToken " + JSON.stringify(err));
              });
          },
        })
          .then((data) => {
            console.log("initialize", data);
            return data.secret;
          })
          .catch((err) => {
            console.log("initialize error", err);
            // alert("initialize: " + JSON.stringify(err));
          });
      })
      .catch((err) => {
        alert("Location permission is required " + err);
      });
  }

  componentWillUnmount() {
    this.discoverListener.remove();
    this.connectionStatusListener.remove();
    this.disconnectListener.remove();
    this.logListener.remove();
    this.inputListener.remove();
    this.updateListener.remove();
    this.startInstallingUpdateListener.remove();
    this.didReportReaderSoftwareUpdateProgressListener.remove();
    this.finishInstallingUpdateListener.remove();
    StripeTerminal.abortDiscoverReaders();
  }

  discover() {
    this.setState({ completedPayment: "discovery..." });

    StripeTerminal.discoverReaders(
      //StripeTerminal.DeviceTypeReaderSimulator,
      // StripeTerminal.DeviceTypeChipper2X,
      // StripeTerminal.DiscoveryMethodBluetoothProximity
      this.state.discoveryMethod,
      this.state.isSimulated ? 1 : 0,
      this.state.locationId
    )
      .then((readers) => {
        console.log("discover readers complete", JSON.stringify(readers));
      })
      .catch((err) => {
        console.log("error", err);
        // alert("discover readers error: " + JSON.stringify(err));
      });
    console.log("discoverReaders");
  }

  createPayment() {
    this.setState({ completedPayment: "creating payment Intent" });
    fetch(this.BACKEND_URL + "/create_payment_intent", {
      method: "POST",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        amount: Math.round(2 * 100),
        currency: "GBP",
        payment_method_types: ["card_present"],
        capture_method: "manual",
      }),
    })
      .then((data) => data.json())
      .then((paymentIntent) => {
        this.setState({ completedPayment: "created payment Intent" });
        console.log("creating intent", paymentIntent);
        StripeTerminal.retrievePaymentIntent(paymentIntent.client_secret)
          .then(() => {
            this.setState({ completedPayment: "retrieved payment Intent" });
            StripeTerminal.collectPaymentMethod()
              .then((intent) => {
                console.log("payment method", intent);
                this.setState({ completedPayment: "collected payment method" });
                StripeTerminal.processPayment()
                  .then((intent) => {
                    this.setState({ completedPayment: "payment processed" });
                    console.log("payment success", intent.stripeId);
                    fetch(this.BACKEND_URL + "/capture_payment_intent", {
                      method: "POST",
                      headers: {
                        Accept: "application/json",
                        "Content-Type": "application/json",
                      },
                      body: JSON.stringify({ id: intent.stripeId }),
                    })
                      .then((resp) => {
                        console.log("got data capture", resp);
                        this.setState({
                          completedPayment: "payment completed",
                        });
                      })
                      .catch((err) => {
                        console.log("capture error", err);
                        this.setState({ completedPayment: err });
                        // alert("capture error " + JSON.stringify(err));
                      });
                  })
                  .catch((err) => {
                    this.setState({ completedPayment: err });
                    // alert("process payment error " + JSON.stringify(err));
                  });
              })
              .catch((err) => {
                this.setState({ completedPayment: err });
                // alert("collect payment method error " + JSON.stringify(err));
              });
          })
          .catch((err) => {
            console.log("retrieve payment intent error", err);
            this.setState({ completedPayment: err });
            // alert("retrieve error " + JSON.stringify(err));
          });
      })
      .catch((err) => {
        this.setState({ completedPayment: err });
        // alert("create payment intent error " + JSON.stringify(err));
      });
  }

  render() {
    return (
      <View style={styles.container}>
        {/* <Text style={styles.welcome}>{this.state.displayText}</Text> */}
        <Text style={styles.instructions}>
          Connected: {this.state.connectedReader}
        </Text>
        <Text style={styles.welcome}>
          {JSON.stringify(this.state.completedPayment)}
        </Text>
        <View style={{ flexDirection: "row" }}>
          <TextInput
            style={styles.input}
            onChangeText={(value) => {
              this.setState({ locationId: value });
            }}
            value={this.state.locationId}
            placeholder="tml_*******"
          />
          <TouchableOpacity style={styles.btn} onPress={this.discover}>
            <Text style={styles.btnText}>Discover readers</Text>
          </TouchableOpacity>
        </View>

        <TouchableOpacity style={styles.btn} onPress={this.createPayment}>
          <Text style={styles.btnText}>Pay</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.btn}
          onPress={() => {
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
          }}
        >
          <Text style={styles.btnText}>Update</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.btn}
          onPress={() => {
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
          }}
        >
          <Text style={styles.btnText}>Abort update</Text>
        </TouchableOpacity>

        <View
          style={{
            flexDirection: "row",
            flex: 1,
            justifyContent: "space-between",
            alignItems: "center",
          }}
        >
          <Text style={{ color: "black" }}>simulated</Text>
          <Switch
            trackColor={{ false: "#767577", true: "#81b0ff" }}
            thumbColor={this.state.isSimulated ? "#f5dd4b" : "#f4f3f4"}
            ios_backgroundColor="#3e3e3e"
            onValueChange={() => {
              this.setState({
                isSimulated: !this.state.isSimulated,
              });
            }}
            value={this.state.isSimulated}
          />
        </View>
        <View
          style={{
            flexDirection: "row",
            flex: 1,
            justifyContent: "space-between",
            alignItems: "center",
            marginTop: 20,
          }}
        >
          <Text style={{ color: "black" }}>Bluetooth</Text>
          <Switch
            trackColor={{ false: "#767577", true: "#81b0ff" }}
            thumbColor={
              this.state.discoveryMethod === 0 ? "#f5dd4b" : "#f4f3f4"
            }
            ios_backgroundColor="#3e3e3e"
            onValueChange={() => {
              this.setState({
                discoveryMethod: this.state.discoveryMethod === 0 ? 1 : 0,
              });
            }}
            value={Boolean(!this.state.discoveryMethod)}
          />
        </View>
      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    backgroundColor: "#F5FCFF",
  },
  welcome: {
    fontSize: 20,
    textAlign: "center",
    margin: 10,
  },
  instructions: {
    textAlign: "center",
    color: "#333333",
    marginBottom: 5,
  },
  btn: {
    backgroundColor: "#9932CC",
    borderColor: "grey",
    borderWidth: 1,
    marginVertical: 10,
    padding: 10,
  },
  btnText: {
    textAlign: "center",
    color: "white",
  },
  input: {
    height: 40,
    margin: 12,
    borderWidth: 1,
    padding: 10,
  },
});
