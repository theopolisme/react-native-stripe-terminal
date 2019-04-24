import { useEffect, useState, useRef } from "react";

export default function createHooks(StripeTerminal) {

  function useStripeTerminalState() {
    const [connectionStatus, setConnectionStaus] = useState(StripeTerminal.ConnectionStatusNotConnected);
    const [paymentStatus, setPaymentStatus] = useState(StripeTerminal.PaymentStatusNotReady);
    const [lastReaderEvent, setLastReaderEvent] = useState(StripeTerminal.ReaderEventCardRemoved);
    const [connectedReader, setConnectedReader] = useState(null);
    const [readerInputOptions, setReaderInputOptions] = useState(null);
    const [readerInputPrompt, setReaderInputPrompt] = useState(null);

    useEffect(() => {
      // Populate initial values
      StripeTerminal.getConnectionStatus().then(s => setConnectionStaus(s));
      StripeTerminal.getPaymentStatus().then(s => setPaymentStatus(s));
      StripeTerminal.getLastReaderEvent().then(e => setLastReaderEvent(e));
      StripeTerminal.getConnectedReader().then(r => setConnectedReader(r));

      // Setup listeners
      const listeners = [
        StripeTerminal.addDidChangeConnectionStatusListener(({ status }) => {
          setConnectionStaus(status);
          StripeTerminal.getConnectedReader().then(r => setConnectedReader(r));
        }),
        StripeTerminal.addDidChangePaymentStatusListener(({ status }) => setPaymentStatus(status)),
        StripeTerminal.addDidReportReaderEventListener(({ event }) => setLastReaderEvent(event)),
        StripeTerminal.addDidBeginWaitingForReaderInputListener(({ text }) => setReaderInputOptions(text)),
        StripeTerminal.addDidRequestReaderInputPromptListener(({ text }) => setReaderInputPrompt(text))
      ];

      // Cleanup: remove listeners
      return () => {
        listeners.forEach(l => l.remove());
      };
    }, []);

    const cardInserted = lastReaderEvent === StripeTerminal.ReaderEventCardInserted;

    return {
      connectionStatus,
      connectedReader,
      paymentStatus,
      readerInputOptions,
      readerInputPrompt,
      cardInserted
    };
  }

  function useStripeTerminalCreatePayment({ onSuccess, onFailure, ...options }) {
    const {
      connectionStatus,
      connectedReader,
      paymentStatus,
      cardInserted,
      readerInputOptions,
      readerInputPrompt
    } = state = useStripeTerminalState();

    const [hasCreatedPayment, setHasCreatedPayment] = useState(false);
    const [isCompleted, setIsCompleted] = useState(false);

    useEffect(() => {
      if (paymentStatus === StripeTerminal.PaymentStatusReady && !hasCreatedPayment) {
        setHasCreatedPayment(true);
        StripeTerminal.createPayment(options)
          .then(onSuccess)
          .catch(onFailure)
          .finally(() => setIsCompleted(true));
      }
    }, [paymentStatus]);

    // Cleanup: abort if unmounted midway through payment intent creation process.
    useEffect(() => {
      return () => {
        if (!isCompleted) {
          StripeTerminal.abortCreatePayment();
        }
      };
    }, []);

    return {
      ...state
    };
  }

  return {
    useStripeTerminalState,
    useStripeTerminalCreatePayment
  };
}
