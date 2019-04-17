import { useEffect, useState, useRef } from "react";

export function useStripeTerminalState() {
  const [connectionStatus, setConnectionStaus] = useState(this.ConnectionStatusNotConnected);
  const [paymentStatus, setPaymentStaus] = useState(this.PaymentStatusNotReady);
  const [lastReaderEvent, setLastReaderEvent] = useState(this.ReaderEventCardRemoved);
  const [connectedReader, setConnectedReader] = useState(null);

  useEffect(() => {
    const listeners = [
      this.addDidChangeConnectionStatusListener(s => {
        setConnectionStaus(s);
        this.getConnectedReader().then(r => setConnectedReader(r));
      }),
      this.addDidChangePaymentStatusListener(s => setPaymentStaus(s)),
      this.addDidReportReaderEventListener(e => setLastReaderEvent(e))
    ];

    return () => {
      listeners.forEach(l => l.remove());
    };
  }, []);

  const cardInserted = lastReaderEvent === this.ReaderEventCardInserted;

  return {
    connectionStatus,
    connectedReader,
    paymentStatus,
    cardInserted
  };
}

export function useStripeTerminalConnectReader() {

}

export function useStripeTerminalCreatePayment() {

}
