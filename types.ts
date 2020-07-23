// @ts-ignore
import EventEmitter from 'eventemitter3';

export interface Reader {
  serialNumber?: string;
}

export interface ServiceOptions {
  policy: string;
  deviceType: string;
  discoveryMode: string;
  simulated: number;
  emitter: EventEmitter;
  desiredReader: null | string;
}
