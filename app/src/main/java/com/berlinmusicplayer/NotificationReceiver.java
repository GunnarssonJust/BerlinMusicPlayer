package com.berlinmusicplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import static com.berlinmusicplayer.ApplicatonClass.ACTION_CANCEL;
import static com.berlinmusicplayer.ApplicatonClass.ACTION_NEXT;
import static com.berlinmusicplayer.ApplicatonClass.ACTION_PLAY;
import static com.berlinmusicplayer.ApplicatonClass.ACTION_PREV;


public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String actionName = intent.getAction();
        Intent serviceIntent = new Intent(context, MusicService.class);

        if (actionName == null) return;

        switch (actionName) {
            case ACTION_PREV:
                serviceIntent.setAction(ACTION_PREV);
                break;
            case ACTION_PLAY:
                serviceIntent.setAction(ACTION_PLAY);
                break;
            case ACTION_NEXT:
                serviceIntent.setAction(ACTION_NEXT);
                break;
            case ACTION_CANCEL:
                serviceIntent.setAction(ACTION_CANCEL);
                break;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
