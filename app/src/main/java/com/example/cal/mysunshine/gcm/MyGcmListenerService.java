package com.example.cal.mysunshine.gcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.example.cal.mysunshine.MainActivity;
import com.example.cal.mysunshine.R;
import com.google.android.gms.gcm.GcmListenerService;

import org.json.JSONException;
import org.json.JSONObject;

public class MyGcmListenerService extends GcmListenerService {
    private static final String TAG = MyGcmListenerService.class.getSimpleName();

    private static final String EXTRA_DATA = "data";
    private static final String EXTRA_WEATHER = "weather";
    private static final String EXTRA_LOCATION = "location";

    public static final int NOTIFICATION_ID = 1;

    @Override
    public void onMessageReceived(String s, Bundle bundle) {
        if (!bundle.isEmpty()) {
            if (getString(R.string.gcm_defaultSenderId).equals(s)) {

                String weather = bundle.getString(EXTRA_WEATHER);
                String location = bundle.getString(EXTRA_LOCATION);
                String alert = String.format(getString(R.string.gcm_weather_alert), weather, location);
                sendNotification(alert);

            }
            Log.i(TAG, "Received: " + bundle.toString());
        }
    }

    private void sendNotification(String message) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);

        Bitmap largeIcon = BitmapFactory.decodeResource(this.getResources(), R.drawable.art_storm);
        NotificationCompat.Builder noteBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.art_clear)
                .setLargeIcon(largeIcon)
                .setContentTitle("Weather Alert!!!")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi);
        nm.notify(NOTIFICATION_ID, noteBuilder.build());
    }
}
