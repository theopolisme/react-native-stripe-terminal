import EventEmitter from 'EventEmitter';
import AsyncStorage from '@react-native-community/async-storage';

export default function createConnectionService(StripeTerminal, options) {
 
  class StripeTerminalConnectionService {

    static StorageKey = '@StripeTerminalConnectionService:persistedSerialNumber';

    static EventConnectionError = 'connectionError';
    static EventPersistedReaderNotFound = 'persistedReaderNotFound';
    static EventLog = 'log';

    static PolicyAuto = 'auto';
    static PolicyPersist = 'persist';
    static PolicyManual = 'manual';
    static Policies = [
      StripeTerminalConnectionService.PolicyAuto,
      StripeTerminalConnectionService.PolicyPersist,
      StripeTerminalConnectionService.PolicyManual
    ];

    static DesiredReaderAny = 'any';

    constructor({ policy, deviceType, discoveryMode }) {
      this.policy = policy;
      this.deviceType = deviceType || StripeTerminal.DeviceTypeChipper2X;
      this.discoveryMode = discoveryMode || StripeTerminal.DiscoveryMethodBluetoothProximity;

      if (StripeTerminalConnectionService.Policies.indexOf(policy) === -1) {
        throw new Error(`Invalid policy passed to StripeTerminalConnectionService: got "${policy}", expects "${
          StripeTerminalConnectionService.Policies.join('|')}"`);
      }

      this.emitter = new EventEmitter();
      this.desiredReader = null;

      this.listeners = [
        StripeTerminal.addReadersDiscoveredListener(this.onReadersDiscovered.bind(this)),
        StripeTerminal.addDidDisconnectUnexpectedlyFromReaderListener(this.onUnexpectedDisconnect.bind(this))
      ];
    }

    onReadersDiscovered(readers) {
      if (!readers.length) { return; }

      let connectionPromise;

      // Auto-reconnect to "desired" reader, if one exists. This could happen
      // if the connection drops, for example. Or when restoring from memory.
      const foundReader = readers.find(r => r.serialNumber === this.desiredReader);
      if (foundReader) {
        connectionPromise = StripeTerminal.connectReader(foundReader.serialNumber);

      // Otherwise, connect to best strength reader.
      } else if (this.policy === StripeTerminalConnectionService.PolicyAuto ||
        (this.policy === StripeTerminalConnectionService.PolicyPersist && !this.desiredReader) ||
        this.desiredReader === StripeTerminalConnectionService.DesiredReaderAny)
      {
        connectionPromise = StripeTerminal.connectReader(readers[0].serialNumber);
      }

      // If a connection is in progress, save the connected reader.
      if (connectionPromise) {
        connectionPromise.then(r => {
          this.desiredReader = r.serialNumber;

          if (this.policy === StripeTerminalConnectionService.PolicyPersist) {
            AsyncStorage.setItem(StripeTerminalConnectionService.StorageKey, this.desiredReader);
          }
        }).catch(e => {
          // If unable to connect, emit error & restart if in automatic mode.
          this.emitter.emit(StripeTerminalConnectionService.EventConnectionError, e);
          if (this.policy !== StripeTerminalConnectionService.PolicyManual) { this.connect(); }
        });

      // If the only reader found was not an "allowed" persisted reader, restart the search.
      } else {
        this.emitter.emit(StripeTerminalConnectionService.EventPersistedReaderNotFound, readers);
        this.connect();
      }
    }

    onUnexpectedDisconnect() {
      // Automatically attempt to reconnect.
      this.connect();
    }

    async connect(serialNumber) {
      this.emitter.emit(StripeTerminalConnectionService.EventLog, `Connecting to reader: "${serialNumber || 'any'}"...`);

      if (serialNumber) { this.desiredReader = serialNumber; }
      if (!this.desiredReader) { this.desiredReader = StripeTerminalConnectionService.DesiredReaderAny; }

      // Don't reconnect if we are already connected to the desired reader.
      // (This state can occur when hot-reloading, for example.)
      const currentReader = await this.getReader();
      if (currentReader) { return Promise.resolve(); }

      await StripeTerminal.abortDiscoverReaders(); // end any pending search
      await StripeTerminal.disconnectReader(); // cancel any existing non-matching reader
      return StripeTerminal.discoverReaders(this.deviceType, this.discoveryMode);
    }

    disconnect() {
      if (this.policy === StripeTerminalConnectionService.PolicyPersist) {
        AsyncStorage.removeItem(StripeTerminalConnectionService.StorageKey);
        this.desiredReader = null;
      }
      return StripeTerminal.disconnectReader();
    }

    async getReader() {
      const reader = await StripeTerminal.getConnectedReader();
      return (reader && reader.serialNumber === this.desiredReader) ? reader : null;
    }

    addListener(event, handler) {
      return this.emitter.addListener(event, handler);
    }

    async start() {
      if (this.policy === StripeTerminalConnectionService.PolicyAuto) {
        this.connect();

      } else if (this.policy === StripeTerminalConnectionService.PolicyPersist) {
        const serialNumber = await AsyncStorage.getItem(StripeTerminalConnectionService.StorageKey);
        this.connect(serialNumber);

      } else {
        /* fallthrough, on PolicyManual wait for user action */
      }
    }

    async stop() {
      await StripeTerminal.disconnectReader();
      this.listeners.forEach(l => l.remove());
    }
  }

  const service = new StripeTerminalConnectionService(options);
  return service;
}
