package com.berlinmusicplayer;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class ApplicatonClass extends Application {
    public static final String CHANNEL_ID_2 = "CHANNEL_2";
    public static final String ACTION_NEXT = "next";
    public static final String ACTION_PREV = "previous";
    public static final String ACTION_PLAY = "play";
    public static final String ACTION_CANCEL = "cancel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel notificationChannel2 = new NotificationChannel(CHANNEL_ID_2,
                    "RIAS Berlin", NotificationManager.IMPORTANCE_HIGH);
            notificationChannel2.setDescription("Channel 2 Description");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(notificationChannel2);
        }
    }
}
