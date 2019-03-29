/**DiscoveryMethodBluetoothProximity
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 * @flow
 */

import React, {Component} from 'react';
import {Platform, StyleSheet, Text, View} from 'react-native';
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
      displayText: "Loading stuff..."
    };

    StripeTerminal.initialize({
      fetchConnectionToken: () => {
        return fetch('http://10.0.1.46:8080/_scanner/terminal_connection_token?api_key=2e21c24db5f8bfae31cf420b60f45df6', {
          method: 'POST'
        })
        .then(resp => resp.json())
        .then(data => {
          console.log('got data', data);
          return data.secret;
        });
      }
    });

    var isConnecting = false;

    StripeTerminal.addDidBeginWaitingForReaderInputListener(text => {
      this.setState({ displayText: text });
    });

    StripeTerminal.addDidRequestReaderInputPrompt(text => {
      this.setState({ displayText: text });
    });

    StripeTerminal.addReadersDiscoveredListener(readers => {
      console.log('readers discovered', readers);
      if (readers.length && !isConnecting) {
        isConnecting = true;
        StripeTerminal.connectReader(readers[0].serialNumber)
          .then(() => {
            console.log('reader connected');
            StripeTerminal.createPaymentIntent({ amount: 1200, currency: "usd" })
            .then(intent => {
              console.log('wowee, we did it', intent);
            })
            .catch(err => {
              console.log('pay failed', err);
            });
           }).catch(e => console.log('failed to connect', e));
      }
    });

    StripeTerminal.discoverReaders(StripeTerminal.DeviceTypeReaderSimulator,
      // StripeTerminal.DeviceTypeChipper2X,
      StripeTerminal.DiscoveryMethodBluetoothProximity);
  }

  render() {
    return (
      <View style={styles.container}>
        <Text style={styles.welcome}>{this.state.displayText}</Text>
        <Text style={styles.instructions}>Yeehaw this is Stripeyyy</Text>
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
