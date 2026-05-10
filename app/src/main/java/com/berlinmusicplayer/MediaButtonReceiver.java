package com.berlinmusicplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.KeyEvent;

public class MediaButtonReceiver extends BroadcastReceiver {

    private static final String TAG = "MediaButtonReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Sicherstellen, dass der Intent der richtige ist
        if (intent == null || !Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            return;
        }

        // Das KeyEvent-Objekt aus dem Intent extrahieren
        KeyEvent keyEvent = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        if (keyEvent == null || keyEvent.getAction() != KeyEvent.ACTION_DOWN) {
            // Wir reagieren nur auf das "Drücken", nicht auf das "Loslassen"
            return;
        }

        // Erstellen eines Intents, um den Befehl an unseren MusicService zu senden
        Intent serviceIntent = new Intent(context, MusicService.class);
        String command = null;

        // Den Tastencode auswerten
        switch (keyEvent.getKeyCode()) {
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                Log.d(TAG, "Bluetooth: PLAY empfangen");
                serviceIntent.putExtra("actionName", "play");
                break;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                Log.d(TAG, "Bluetooth: PAUSE empfangen");
                serviceIntent.putExtra("actionName", "pause");
                // Unser Service toggelt, also ist "play" ausreichend
                break;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                Log.d(TAG, "Bluetooth: NEXT empfangen");
                serviceIntent.putExtra("actionName", "next");
                break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                Log.d(TAG, "Bluetooth: PREVIOUS empfangen");
                serviceIntent.putExtra("actionName", "previous");
                break;
            case KeyEvent.KEYCODE_HEADSETHOOK: // Viele Headsets senden diesen Code für Play/Pause
                Log.d(TAG, "Bluetooth: HEADSETHOOK empfangen");
                serviceIntent.putExtra("actionName", "play");
                break;
            default:
                return; // Unbekannte Taste, nichts tun
        }

        // Den Befehl an den MusicService senden, damit dieser die Aktion ausführt
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            context.startForegroundService(serviceIntent);
        }else{
            context.startService(serviceIntent);
        }
    }
}