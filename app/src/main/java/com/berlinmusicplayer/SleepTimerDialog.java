package com.berlinmusicplayer;

import android.content.Context;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

public class SleepTimerDialog {
    private SleepTimerDialog() {}
    public static void show(Context ctx, MusicService service){
        if(service == null){
            Toast.makeText(ctx, "Kein Service verbunden", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] times = {"5 min","15 min", "30 min", "45 min", "60 min","Aus"};
        long[] ms = {5 * 60 * 1000L, 15 * 60 * 1000L, 30 * 60 * 1000L, 45 * 60 * 1000L, 60 * 60 * 1000L, 0
        };
        new AlertDialog.Builder(ctx,R.style.SleepTimerDialogTheme)
                .setTitle("Schlafzeit-Uhr")
                .setSingleChoiceItems(times,-1,(d,which)-> {
                    if(ms[which] == 0){
                        service.stopSleepTimer();
                        Toast.makeText(ctx, "Schlafzeit-Uhr deaktiviert", Toast.LENGTH_SHORT).show();
                    }else{
                        service.startSleepTimer(ms[which]);
                        Toast.makeText(ctx, "Schlafzeit-Uhr aktiviert", Toast.LENGTH_SHORT).show();
                    }
                    d.dismiss();
                }).show();
    }
}
