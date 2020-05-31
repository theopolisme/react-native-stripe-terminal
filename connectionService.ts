// @ts-ignore
import EventEmitter from 'eventemitter3';
// @ts-ignore
import AsyncStorage from '@react-native-community/async-storage';

export default function createConnectionService(StripeTerminal, options) {
  class STCS {
    static StorageKey = '@STCS:persistedSerialNumber';

    static EventConnectionError = 'connectionError';
    static EventPersistedReaderNotFound = 'persistedReaderNotFound';
    static EventReadersDiscovered = 'readersDiscovered';
    static EventReaderPersisted = 'readerPersisted';
    static EventLog = 'log';

    static PolicyAuto = 'auto';
    static PolicyPersist = 'persist';
    static PolicyManual = 'manual';
    static PolicyPersistManual = 'persist-manual';
    static Policies = [
      STCS.PolicyAuto,
      STCS.PolicyPersist,
      STCS.PolicyManual,
      STCS.PolicyPersistManual,
    ];

    static DesiredReaderAny = 'any';

    policy: string;
    deviceType: string;
    discoveryMode: string;
    simulated: number;
    emitter: EventEmitter;
    desiredReader: null | string;

    constructor({ policy, deviceType, discoveryMode, simulated }) {
      this.policy = policy;
      this.deviceType = deviceType || StripeTerminal.DeviceTypeChipper2X;
      this.discoveryMode = discoveryMode || StripeTerminal.DiscoveryMethodBluetoothProximity;
      this.simulated = simulated || 0;

      if (STCS.Policies.indexOf(policy) === -1) {
        throw new Error(
          `Invalid policy passed to STCS: got "${policy}", expects "${STCS.Policies.join('|')}"`,
        );
      }

      this.emitter = new EventEmitter();
      this.desiredReader = null;

      StripeTerminal.addReadersDiscoveredListener(this.onReadersDiscovered);
      StripeTerminal.addDidReportUnexpectedReaderDisconnectListener(this.onUnexpectedDisconnect);
    }

    onReadersDiscovered = (readers) => {
      this.emitter.emit(STCS.EventReadersDiscovered, readers);

      if (!readers.length) {
        return;
      }

      // If we are not currently in a connecting phase, just emit the found readers without
      // connecting to anything (that will wait until the connect() call).
      if (!this.desiredReader) {
        return;
      }

      let connectionPromise;

      // Auto-reconnect to "desired" reader, if one exists. This could happen
      // if the connection drops, for example. Or when restoring from memory.
      const foundReader = readers.find((r) => r.serialNumber === this.desiredReader);
      if (foundReader) {
        connectionPromise = StripeTerminal.connectReader(foundReader.serialNumber);

        // Otherwise, connect to best strength reader.
      } else if (
        this.policy === STCS.PolicyAuto ||
        (this.policy === STCS.PolicyPersist && !this.desiredReader) ||
        this.desiredReader === STCS.DesiredReaderAny
      ) {
        connectionPromise = StripeTerminal.connectReader(readers[0].serialNumber);
      }

      // If a connection is in progress, save the connected reader.
      if (connectionPromise) {
        connectionPromise
          .then((r) => {
            this.desiredReader = r.serialNumber;

            if (this.policy === STCS.PolicyPersist || this.policy === STCS.PolicyPersistManual) {
              this.setPersistedReaderSerialNumber(this.desiredReader);
            }
          })
          .catch((e) => {
            // If unable to connect, emit error & restart if in automatic mode.
            this.emitter.emit(STCS.EventConnectionError, e);
            if (this.policy !== STCS.PolicyManual) {
              this.connect();
            }
          });

        // If the only reader found was not an "allowed" persisted reader, restart the search.
      } else {
        this.emitter.emit(STCS.EventPersistedReaderNotFound, readers);
        this.connect();
      }
    };

    onUnexpectedDisconnect = () => {
      // Automatically attempt to reconnect.
      this.connect();
    };

    async connect(serialNumber?: string) {
      this.emitter.emit(STCS.EventLog, `Connecting to reader: "${serialNumber || 'any'}"...`);

      if (serialNumber) {
        this.desiredReader = serialNumber;
      }
      if (!this.desiredReader) {
        this.desiredReader = STCS.DesiredReaderAny;
      }

      // Don't reconnect if we are already connected to the desired reader.
      // (This state can occur when hot-reloading, for example.)
      const currentReader = await this.getReader();
      if (currentReader) {
        return Promise.resolve();
      }

      await StripeTerminal.abortDiscoverReaders(); // end any pending search
      await StripeTerminal.disconnectReader(); // cancel any existing non-matching reader
      return StripeTerminal.discoverReaders(this.deviceType, this.discoveryMode, this.simulated);
    }

    async discover() {
      await StripeTerminal.abortDiscoverReaders(); // end any pending search
      return StripeTerminal.discoverReaders(this.deviceType, this.discoveryMode, this.simulated);
    }

    async disconnect() {
      if (this.policy === STCS.PolicyPersist || this.policy === STCS.PolicyPersistManual) {
        await this.setPersistedReaderSerialNumber(null);
        this.desiredReader = null;
      }
      return StripeTerminal.disconnectReader();
    }

    async getReader() {
      const reader = await StripeTerminal.getConnectedReader();
      return reader && reader.serialNumber === this.desiredReader ? reader : null;
    }

    addListener(event, handler) {
      return this.emitter.addListener(event, handler);
    }

    async getPersistedReaderSerialNumber() {
      return AsyncStorage.getItem(STCS.StorageKey);
    }

    async setPersistedReaderSerialNumber(serialNumber) {
      if (!serialNumber) {
        await AsyncStorage.removeItem(STCS.StorageKey);
      } else {
        await AsyncStorage.setItem(STCS.StorageKey, serialNumber);
      }
      this.emitter.emit(STCS.EventReaderPersisted, serialNumber);
    }

    async start() {
      if (this.policy === STCS.PolicyAuto) {
        this.connect();
      } else if (this.policy === STCS.PolicyPersist || this.policy === STCS.PolicyPersistManual) {
        const serialNumber = await this.getPersistedReaderSerialNumber();
        if (this.policy === STCS.PolicyPersist || serialNumber) {
          this.connect(serialNumber);
        }
      } else {
        /* fallthrough, on PolicyManual, or PolicyPersistManual with no found reader, wait for user action */
      }
    }

    async stop() {
      await StripeTerminal.disconnectReader();

      StripeTerminal.removeReadersDiscoveredListener(this.onReadersDiscovered);
      StripeTerminal.removeDidReportUnexpectedReaderDisconnectListener(this.onUnexpectedDisconnect);
    }
  }

  const StripeTerminalConnectionService = STCS;
  return new StripeTerminalConnectionService(options);
}
