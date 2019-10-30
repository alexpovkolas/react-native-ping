declare module "react-native-ping" {

    export type PingStatus = 'Pending' | 'Success' | 'Fail'

    export interface PingResponse {
        sequenceNumber: number;
        ttl: number;
        rtt: number;
        status: PingStatus;
    }

    export interface RequestOptions {
        pingPeriod?: number;
        timeout?: number;
        payloadSize?: number;
        ttl?: number;
    }

    export class Ping  {
        readonly host: string;
        readonly options?: RequestOptions;

        constructor(host: string, count?: number, options?: RequestOptions);

        start(callback: (region: PingResponse) => void): void;
        stop(): void;
    }

}