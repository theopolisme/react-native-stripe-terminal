/**DiscoveryMethodBluetoothProximity
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 * @flow
 */

import React, {Component} from 'react';
import {Platform, StyleSheet, Text, View, TouchableOpacity} from 'react-native';
import StripeTerminal from './StripeTerminal.js';

const instructions = Platform.select({
  ios: 'Press Cmd+R to reload,\n' + 'Cmd+D or shake for dev menu',
  android:
    'Double tap R on your keyboard to reload,\n' +
    'Shake or press menu button for dev menu',
});

export default class App extends Component {
  constructor(props) {
    super(props);

    this.state = {
      isConnecting: false,
      readerConnected: false,
      completedPayment: null,
      displayText: "Loading..."
    };

    this.discover = this.discover.bind(this);
    this.createPayment = this.createPayment.bind(this);

    StripeTerminal.initialize({
      fetchConnectionToken: () => {
        return fetch('http://10.0.1.35:8080/_scanner/terminal_connection_token?api_key=2e21c24db5f8bfae31cf420b60f45df6', {
          method: 'POST'
        })
        .then(resp => resp.json())
        .then(data => {
          console.log('got data', data);
          return data.secret;
        });
      }
    });

    StripeTerminal.addDidChangeConnectionStatusListener(({ status }) => {
      console.log("status change", status);
      this.setState({ readerConnected: status === StripeTerminal.ConnectionStatusConnected });
    });

    StripeTerminal.addDidDisconnectUnexpectedlyFromReaderListener(() => {
      this.setState({ displayText: 'Disconnected unexpectedly! Oh noez' });
    })

    StripeTerminal.addDidBeginWaitingForReaderInputListener(({ text }) => {
      this.setState({ displayText: text });
    });

    StripeTerminal.addDidRequestReaderInputPromptListener(({ text }) => {
      this.setState({ displayText: text });
    });

    StripeTerminal.addReadersDiscoveredListener(readers => {
      if (readers.length && !this.state.readerConnected && !this.state.isConnecting) {
        this.setState({ isConnecting: true });
        StripeTerminal.connectReader(readers[0].serialNumber)
          .then(() => {
            this.setState({ isConnecting: false });
          }).catch(e => console.log('failed to connect', e));
      }
    });
  }

  discover() {
    this.setState({ completedPayment: 'discovery...' });

    StripeTerminal.discoverReaders(
      //StripeTerminal.DeviceTypeReaderSimulator,
      StripeTerminal.DeviceTypeChipper2X,
      StripeTerminal.DiscoveryMethodBluetoothProximity
    );
  }

  createPayment() {
    StripeTerminal.createPaymentIntent({ amount: 1200, currency: "usd" })
      .then(intent => {
        this.setState({ completedPayment: intent });
      })
      .catch(err => {
        this.setState({ completedPayment: err });
      });
  }

  render() {
    return (
      <View style={styles.container}>
        <Text style={styles.welcome}>{this.state.displayText}</Text>
        <Text style={styles.instructions}>Connected: {this.state.readerConnected}</Text>
        <Text style={styles.instructions}>{JSON.stringify(this.state.completedPayment)}</Text>

        <TouchableOpacity onPress={this.discover}>
          <Text>Discover readers</Text>
        </TouchableOpacity>
        <TouchableOpacity onPress={this.createPayment}>
          <Text>Create payment</Text>
        </TouchableOpacity>
      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
  },
  welcome: {
    fontSize: 20,
    textAlign: 'center',
    margin: 10,
  },
  instructions: {
    textAlign: 'center',
    color: '#333333',
    marginBottom: 5,
  },
});
