import { useEffect, useState, useRef } from "react";
import { cancelable } from 'cancelable-promise';


export default function createHooks(StripeTerminal) {

  function useStripeTerminalState() {
    const [connectionStatus, setConnectionStaus] = useState(StripeTerminal.ConnectionStatusNotConnected);
    const [paymentStatus, setPaymentStatus] = useState(StripeTerminal.PaymentStatusNotReady);
    const [lastReaderEvent, setLastReaderEvent] = useState(StripeTerminal.ReaderEventCardRemoved);
    const [connectedReader, setConnectedReader] = useState(null);
    const [readerInputOptions, setReaderInputOptions] = useState(null);
    const [readerInputPrompt, setReaderInputPrompt] = useState(null);
    const isMounted = useRef(true);

    useEffect(() => {
      // Populate initial values
      const p1 = cancelable(StripeTerminal.getConnectionStatus())
      p1.then(s => {
        if (isMounted.current) {
          setConnectionStaus(s);
        }
      });
      const p2 = cancelable(StripeTerminal.getPaymentStatus().then(s => {
        if (isMounted.current) {
          setPaymentStatus(s);
        }
      })).catch(e => {});
      const p3 = cancelable(StripeTerminal.getLastReaderEvent().then(e => {
        if (isMounted.current) {
          setLastReaderEvent(e);
        }
      })).catch(e => {});
      const p4 = cancelable(StripeTerminal.getConnectedReader().then(r => {
        if (isMounted.current) {
          setConnectedReader(r);
        }
      })).catch(e => {});

      let p5 = null;
      const didChangeConnectionStatus = ({ status }) => {
        if (isMounted.current) {
          setConnectionStaus(status);
          p5 = cancelable(StripeTerminal.getConnectedReader().then(r => {
            if (isMounted.current) {
              setLastReaderEvent(StripeTerminal.ReaderEventCardRemoved);
              setConnectedReader(r);
            }
          })).catch(e => {});
        } 
      };
      const didChangePaymentStatus = ({ status }) => {
        if (isMounted.current) {
          setPaymentStatus(status);
        }
      };
      const didReportReaderEvent = ({ event }) => {
        if (isMounted.current) {
          setLastReaderEvent(event);
        }
      };
      const didBeginWaitingForReaderInput = ({ text }) => {
        if (isMounted.current) {
          setReaderInputOptions(text);
        }
      };
      const didRequestReaderInput = ({ text }) => {
        if (isMounted.current) {
          setReaderInputPrompt(text);
        }
      };

      // Setup listeners
      StripeTerminal.addDidChangeConnectionStatusListener(didChangeConnectionStatus);
      StripeTerminal.addDidChangePaymentStatusListener(didChangePaymentStatus);
      StripeTerminal.addDidReportReaderEventListener(didReportReaderEvent);
      StripeTerminal.addDidBeginWaitingForReaderInputListener(didBeginWaitingForReaderInput);
      StripeTerminal.addDidRequestReaderInputListener(didRequestReaderInput);

      // Cleanup: remove listeners
      return () => {
        StripeTerminal.removeDidChangeConnectionStatusListener(didChangeConnectionStatus);
        StripeTerminal.removeDidChangePaymentStatusListener(didChangePaymentStatus);
        StripeTerminal.removeDidReportReaderEventListener(didReportReaderEvent);
        StripeTerminal.removeDidBeginWaitingForReaderInputListener(didBeginWaitingForReaderInput);
        StripeTerminal.removeDidRequestReaderInputListener(didRequestReaderInput);
        p1.cancel();
        p2.cancel();
        p3.cancel();
        p4.cancel();
        if (!!p5) {
          p5.cancel();
        }
      };
    }, []);

    useEffect(() => {
      return () => {
        isMounted.current = false;
      }
    }, [])

    const cardInserted = lastReaderEvent === StripeTerminal.ReaderEventCardInserted;

    return {
      connectionStatus,
      connectedReader,
      paymentStatus,
      readerInputOptions,
      readerInputPrompt,
      cardInserted,
      clearReaderInputState: () => {
        if (isMounted.current) {
          setReaderInputOptions(null);
          setReaderInputPrompt(null);
        }
      }
    };
  }

  function useStripeTerminalReadPaymentMethod() {
    const {
      cardInserted,
    } = useStripeTerminalState();

    const [isCompleted, setIsCompleted] = useState(true);
    const [paymentMethod, setPaymentMethod] = useState(null);
    const [error, setError] = useState(null);
    const isMounted = useRef(true);
    const busyError = "Could not execute readReusableCard because the SDK is busy with another command: readReusableCard.";

    useEffect(() => {
      let p1;
      if (isCompleted && !cardInserted && isMounted.current) {
        setIsCompleted(false);
        p1 = cancelable(StripeTerminal.readReusableCard()
          .then(method => {
            if (isMounted.current) {
              setError(null);
              setPaymentMethod(method);
              setIsCompleted(true)
              return null;
            }
          }).catch(({ error }) => {
            if (isMounted.current) { 
              setPaymentMethod(null);
              setError(error);
              setIsCompleted(true);
              return null;
            }
          }).finally(() => {
            StripeTerminal.abortReadPaymentMethod();
          }));
      }
      return () => {
        if (!!p1) {
          p1.cancel();
        }
      }
    }, [
      isCompleted,
      cardInserted
    ]);

    useEffect(() => {
      return () => {
        isMounted.current = false;
        StripeTerminal.abortReadPaymentMethod().catch(e => {});
      }
    }, [])

    return {
      error,
      isCompleted,
      paymentMethod,
      cardInserted,
    }

  }

  function useStripeTerminalCreatePayment({ onSuccess, onFailure, onCapture, autoRetry, ...options }) {
    const {
      connectionStatus,
      connectedReader,
      paymentStatus,
      cardInserted,
      readerInputOptions,
      readerInputPrompt,
      clearReaderInputState
    } = state = useStripeTerminalState();

    const [hasCreatedPayment, setHasCreatedPayment] = useState(false);
    const [isCaptured, setIsCaptured] = useState(false);
    const [isCompleted, setIsCompleted] = useState(false);
    const [readerError, setReaderError] = useState(null);
    const [hasRetried, setHasRetried] = useState(false);

    useEffect(() => {

      if (paymentStatus !== StripeTerminal.PaymentStatusNotReady &&
          (!hasCreatedPayment || (readerError && !hasRetried && !cardInserted))) {

        setHasCreatedPayment(true);
        if (readerError) {
          setHasRetried(true);
        }

        StripeTerminal.createPayment(options)
          .then(intent => {
            if (onCapture) {
              return onCapture(intent)
                .then(onSuccess)
                .catch(onFailure);
            }

            onSuccess(intent);
          })
          .catch(({ error }) => {
            if (autoRetry) {
              StripeTerminal.abortCreatePayment()
                .then(() => {
                  clearReaderInputState();
                  setHasRetried(false);
                  setReaderError(error);
                })
                .catch(e => {
                  clearReaderInputState();
                  onFailure(e);
                });
              return;
            }

            onFailure(error);
          })
          .finally(() => setIsCompleted(true));
      }
    }, [paymentStatus, hasCreatedPayment, readerError, hasRetried, cardInserted]);

    // Cleanup: abort if unmounted midway through payment intent creation process.
    useEffect(() => {
      return () => {
        if (!isCompleted) {
          StripeTerminal.abortCreatePayment();
        }
      };
    }, []);

    return {
      ...state,
      readerError
    };
  }

  const ConnectionManagerStatusConnected = 'connected';
  const ConnectionManagerStatusConnecting = 'connecting';
  const ConnectionManagerStatusDisconnected = 'disconnected';
  const ConnectionManagerStatusScanning = 'scanning';

  function useStripeTerminalConnectionManager({ service }) {
    const {
      connectionStatus,
      connectedReader,
      paymentStatus,
    } = state = useStripeTerminalState();
    const isMounted = useRef(true);

    const [managerConnectionStatus, setManagerConnectionStatus] = useState(ConnectionManagerStatusDisconnected);
    const [readersAvailable, setReadersAvailable] = useState([]);
    const [persistedReaderSerialNumber, setPersistedReaderSerialNumber] = useState(null);

    useEffect(() => {

      if (isMounted.current) {
        setManagerConnectionStatus(!!connectedReader ? ConnectionManagerStatusConnected : ConnectionManagerStatusDisconnected);
      }
    }, [connectedReader]);

    useEffect(() => {
      // Populate initial values
      const p1 = cancelable(service.getPersistedReaderSerialNumber()
        .then(s => {
          if (isMounted.current) {
            setPersistedReaderSerialNumber(s)
          }
          return;
        }));
      const readerDiscovered = readers => {
        if (isMounted.current) {
          setReadersAvailable(readers)
        }
      }
      const readerPersisted = serialNumber => {
        if (isMounted.current) {
          setPersistedReaderSerialNumber(serialNumber)
        }
      }

      // Setup listeners
      const listeners = [
        service.addListener('readersDiscovered', readerDiscovered),
        service.addListener('readerPersisted', readerPersisted)
      ];

      // Cleanup: remove listeners
      return () => {
        p1.cancel();
        listeners.forEach(l => l.remove())
      };
    }, []);

    useEffect(() => {
      return () => {
        isMounted.current = false;
      }
    }, [])

    return {
      ...state,
      managerConnectionStatus,
      readersAvailable,
      persistedReaderSerialNumber,
      connectReader: (serialNumber) => {
        if (isMounted.current) {
          setManagerConnectionStatus(ConnectionManagerStatusConnecting);
        }
        service.connect(serialNumber);
      },
      discoverReaders: () => {
        if (isMounted.current) {
          setManagerConnectionStatus(ConnectionManagerStatusScanning);
        }
        return service.discover();
      },
      disconnectReader: () => {
        return service.disconnect();
      },
      abortDiscoverReaders: () => {
        return StripeTerminal.abortDiscoverReaders();
      }
    };
  }

  return {
    useStripeTerminalState,
    useStripeTerminalCreatePayment,
    useStripeTerminalConnectionManager,
    useStripeTerminalReadPaymentMethod
  };
}
