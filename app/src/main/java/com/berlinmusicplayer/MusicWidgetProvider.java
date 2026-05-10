package com.berlinmusicplayer;

import static com.berlinmusicplayer.MusicService.ACTION_UPDATE_WIDGET;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * Implementierung der App Widget Funktionen.
 */
public class MusicWidgetProvider extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        SharedPreferences preferences = context.getSharedPreferences(MusicService.MUSIC_LAST_PLAYED, Context.MODE_PRIVATE);
        String title = preferences.getString(MusicService.SONG_NAME, "Kein Song");
        String artist = preferences.getString(MusicService.ARTIST_NAME, "");
        long albumId = preferences.getLong(MusicService.ALBUM_ID, -1);
        boolean isPlaying = preferences.getBoolean(MusicService.PLAYER_PLAYING, false);

        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
        int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        
        RemoteViews views = getAppropriateLayout(context, minWidth);
        
        views.setTextViewText(R.id.textView_title, title);
        views.setTextViewText(R.id.textView_subtitle, artist);

        // Ihre Icon-Logik bleibt unverändert
        if(isPlaying){
            views.setImageViewResource(R.id.widget_image_playpause, R.drawable.ic_notif_play);
        }else{
            views.setImageViewResource(R.id.widget_image_playpause, R.drawable.ic_notif_pause);
        }

        if(albumId != -1){
            Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
            Uri albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumId);
            views.setImageViewUri(R.id.imageView_cover, albumArtUri);
        }else{
            views.setImageViewResource(R.id.imageView_cover, R.drawable.ic_play_btn);
        }
        
        // Layout-Info wird jetzt übergeben
        bindButtonClickActions(context, views, minWidth);
        
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                           int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        updateAppWidget(context, appWidgetManager, appWidgetId);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
    
    private static RemoteViews getAppropriateLayout(Context context, int minWidth) {
        int thresholdWidth = 220;
        if (minWidth > thresholdWidth){
            return new RemoteViews(context.getPackageName(), R.layout.widget_standard);
        }else{
            return new RemoteViews(context.getPackageName(), R.layout.widget_small);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if(ACTION_UPDATE_WIDGET.equals(intent.getAction())){
            Log.d("MusicWidgetProvider", "onReceive(): Empfang der Daten vom Service");
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);

            if(appWidgetIds != null && appWidgetIds.length > 0) {
                for(int appWidgetId : appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId);
                }
            }
        }
    }

    private static void bindButtonClickActions(Context context, RemoteViews views, int minWidth) {
        final int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

        // OPEN PLAYER
        Intent openIntent = new Intent(context, PlayerActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingOpenSongsIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        views.setOnClickPendingIntent(R.id.widget_player, pendingOpenSongsIntent);

        // PREVIOUS
        Intent intentPrev = new Intent(context, MusicService.class);
        intentPrev.setAction(MusicService.ACTION_PREV);

        PendingIntent pendingPrev = PendingIntent.getService(context, 1, intentPrev, flags);
        views.setOnClickPendingIntent(R.id.widget_image_previous, pendingPrev);

        // PLAY / PAUSE
        Intent intentPlayPause = new Intent(context, MusicService.class);
        intentPlayPause.setAction(MusicService.ACTION_PLAY_PAUSE);

        PendingIntent pendingPlayPause = PendingIntent.getService(context, 2, intentPlayPause, flags);
        views.setOnClickPendingIntent(R.id.widget_image_playpause, pendingPlayPause);

        // NEXT
        Intent intentNext = new Intent(context, MusicService.class);
        intentNext.setAction(MusicService.ACTION_NEXT);

        PendingIntent pendingNext = PendingIntent.getService(context, 3, intentNext, flags);
        views.setOnClickPendingIntent(R.id.widget_image_next, pendingNext);

        // OPTIONAL: Shuffle / Repeat
        Intent intentshuffle = new Intent(context, MusicService.class);
        intentNext.setAction(MusicService.ACTION_SHUFFLE);

        PendingIntent pendingShuffle = PendingIntent.getService(context, 3, intentshuffle, flags);
        views.setOnClickPendingIntent(R.id.widget_image_shuffle, pendingShuffle);

        Intent intentRepeat = new Intent(context, MusicService.class);
        intentNext.setAction(MusicService.ACTION_REPEAT);

        PendingIntent pendingRepeat = PendingIntent.getService(context, 3, intentRepeat, flags);
        views.setOnClickPendingIntent(R.id.widget_image_repeat, pendingRepeat);
    }
}