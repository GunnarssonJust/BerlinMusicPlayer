package com.berlinmusicplayer;

import android.content.Context;
import android.content.Intent;

public class NavigationUtils {
    public static Intent getNowPlayingIntent(Context context) {

        final Intent intent = new Intent(context, PlayerActivity.class);
        intent.setAction(Constants.NAVIGATE_NOWPLAYING);
        return intent;
    }
}
