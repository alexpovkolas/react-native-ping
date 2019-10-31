import { NativeModules, NativeEventEmitter } from 'react-native';
const { RNReactNativePing } = NativeModules;

export class Ping {

  constructor(host, count, options){
    this.host = host;
    this.count = count;
    this.options = options;

    this.pingModuleEmitter = new NativeEventEmitter(RNReactNativePing);
  }

  start(callback) {
    this.subscription = this.pingModuleEmitter.addListener(
      'PingEvent',
      (pingResponse) => {

        if (pingResponse.status && pingResponse.status === 'Success') {
          const result = {
            sequenceNumber: pingResponse.sequenceNumber,
            ttl: pingResponse.ttl,
            rtt: pingResponse.rtt,
            status: pingResponse.status
          };

          callback(result)
        } else {
          callback({status: pingResponse.status ? pingResponse.status : 'Fail'})
        }

      }
    );

    RNReactNativePing.start(this.host, this.count ? this.count : 5, this.options ? this.options : {});
  }

  stop() {
    this.subscription && this.subscription.remove();
    RNReactNativePing.stop();
  }

}

