
package com.reactlibrary;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class RNReactNativePingModule extends ReactContextBaseJavaModule {

    private final String TIMEOUT_KEY = "timeout";
    private final Integer DEFAULT_TIMEOUT = 2000;

    private static final int ERROR = 100;
    private static final int PING = 101;

    private static Handler handlerThread;


    public RNReactNativePingModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @SuppressLint("HandlerLeak")
    @ReactMethod
    public void start(final String host, final Integer count, ReadableMap option, final Promise promise) {

        final Integer timeout = option.hasKey(TIMEOUT_KEY) ?  option.getInt(TIMEOUT_KEY) :  DEFAULT_TIMEOUT;

        handlerThread = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == ERROR){
                    LHDefinition.PING_ERROR_CODE error = LHDefinition.PING_ERROR_CODE.HostErrorUnknown;
                    promise.reject(error.getCode(), error.getMessage());
                }
                else if(msg.what == PING){
                    String line = (String) msg.obj;

                    // looking for something like: 64 bytes from 172.217.16.14: icmp_seq=0 ttl=54 time=30.229 ms
                    if (line != null) {
                        int snIndex  = line.indexOf("icmp_seq=");

                        if (snIndex > 0) {

                            int ttlIndex  = line.indexOf("ttl=");
                            int rttIndex  = line.indexOf("time=");
                            int msIndex  = line.indexOf("ms");

                            if (rttIndex > 0) {
                                Integer sn = Integer.parseInt(line.substring(snIndex + 9, ttlIndex - 1));
                                Integer ttl = Integer.parseInt(line.substring(ttlIndex + 4, rttIndex - 1));
                                Double rtt = Double.parseDouble(line.substring(rttIndex + 5, msIndex - 1));
                                sendEvent(sn, ttl, rtt, "Success");
                            } else {
                                sendEvent(-1, -1, 0.0, "Fail");
                            }
                        }
                    }
                }
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String command =  createSimplePingCommand(count, timeout, host);

                    Process process = Runtime.getRuntime().exec(command);
                    InputStream is = process.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    String line;
                    while (reader.ready() && null != (line = reader.readLine())) {
                        Message message = new Message();
                        message.arg1 = PING;
                        message.obj = line;

                        handlerThread.sendMessage(message);
                    }
                    reader.close();
                    is.close();
                } catch (Exception e) {
                    Message message = new Message();
                    message.arg1 = ERROR;

                    handlerThread.sendMessage(message);
                }
            }
        }).start();

        promise.resolve("Success");
    }

    @ReactMethod
    public void stop() {
        // TODO: implement
    }

    private static String createSimplePingCommand(int count, int timeout, String domain) {
        return "/system/bin/ping -c " + count + " -w " + timeout + " " + domain;
    }

    private void sendEvent(Integer sequenceNumber, Integer ttl, Double rtt, String status) {
        WritableMap params = Arguments.createMap();
        params.putInt("sequenceNumber", sequenceNumber);
        params.putInt("ttl", ttl);
        params.putDouble("rtt", rtt);
        params.putString("status", status);

        getReactApplicationContext()
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("PingEvent", params);
    }

    @Override
    public String getName() {
        return "RNReactNativePing";
    }

}