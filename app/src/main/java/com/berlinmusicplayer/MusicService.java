package com.berlinmusicplayer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import static com.berlinmusicplayer.ApplicatonClass.ACTION_CANCEL;
import static com.berlinmusicplayer.ApplicatonClass.CHANNEL_ID_2;


public class MusicService extends Service implements MediaPlayer.OnCompletionListener, AudioManager
        .OnAudioFocusChangeListener {

    private final IBinder mBinder = new MyBinder();
    private static final String TAG = "MusicService";
    private final boolean D = true;
    private boolean playWhenReady = false;

    MediaPlayer mediaPlayer;
    public MediaSessionCompat mediaSessionCompat;
    private ArrayList<MusicFiles> musicFiles = new ArrayList<>();
    private ArrayList<MusicFiles> currentPlaylist;
    ActionPlaying actionPlaying;
    MusicServiceCallback mainActivityCallback;
    private NotificationManager notificationManager;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private MiniPlayerCallback miniPlayerCallback;
    Uri uri;
    public int position = -1;
    public final int noteId = 24061971;
    public static final String ACTION_UPDATE_WIDGET = "com.berlinmusicplayer.ACTION_UPDATE_WIDGET";
    public static final String ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE";
    public static final String ACTION_NEXT = "ACTION_NEXT";
    public static final String ACTION_PREV = "ACTION_PREV";
    public static final String ACTION_SHUFFLE = "ACTION_SHUFFLE";
    public static final String ACTION_REPEAT = "ACTION_REPEAT";
    public static final String ACTION_OPEN = "ACTION_OPEN";

    public static final String MUSIC_LAST_PLAYED = "LAST_PLAYED";
    public static final String MUSIC_FILE = "STORED_MUSIC";
    public static final String ARTIST_NAME = "ARTIST NAME";
    public static final String SONG_NAME = "SONG NAME";
    public static final String ALBUM_ID = "ALBUM NAME";
    public static final String PLAYER_PLAYING = "PLAYER PLAYING";



    public static boolean repeatBoolean = false;
    public static boolean shuffleBoolean = false;

    // Falls Ohrhörer vom Smartphone oder ein Bluetoothgerät vom Phone getrennt werden,
    // soll die Musik angehalten werden, ein paar Zeilen für eine großartige Sache.
    private final BroadcastReceiver becomingnoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                Log.d(TAG, "MusicService empfängt den Noisy-Broadcast Receiver");

                if (isPlaying()) {
                    playPauseBtnClicked();
                }
            }
        }
    };

    private final IntentFilter becomingNoisyIntentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    public MusicService() {
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "Esel PlayerActivity: onCreate");
        super.onCreate();
        if(musicFiles== null || musicFiles.isEmpty()){
            Log.d(TAG,"MusicService lädt die Musikdaten vom Phone ");
            musicFiles = getAllAudio();
        }
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(this)
                    .build();
        }
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(this,MediaButtonReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent,PendingIntent.FLAG_IMMUTABLE);
        mediaSessionCompat = new MediaSessionCompat(getBaseContext(), "PlayerAudio",null,pendingIntent);
        mediaSessionCompat.setMediaButtonReceiver(pendingIntent);
        mediaSessionCompat.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                super.onPlay();
                playPauseBtnClicked();
            }
            @Override
            public void onPause() {
                super.onPause();
                playPauseBtnClicked();
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                nextBtnClicked();
            }
            @Override
            public void onSeekTo(long pos) {
                if (mediaPlayer != null) {
                    mediaPlayer.seekTo((int) pos);
                    updateMediaSessionState();  // ← Position sofort updaten!
                }
            }
            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                previousBtnClicked();
            }
        });
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        SharedPreferences preferences = getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE);

        // Position validieren
        if (position < 0 || position >= currentPlaylist.size()) {
            position = 0;
        }

        boolean isMasterPlaylist = preferences.getBoolean("isMasterPlaylist", true);
        if(isMasterPlaylist){
            currentPlaylist = musicFiles;
        }else {
            String jsonPaths = preferences.getString("lastPlaylist", null);
            if (jsonPaths != null) {
                // Rekonstruiere die Album-Playlist aus den gespeicherten Pfaden
                Type type = new TypeToken<ArrayList<String>>() {}.getType();
                ArrayList<String> paths = new Gson().fromJson(jsonPaths, type);
                ArrayList<MusicFiles> reconstructedPlaylist = new ArrayList<>();
                // Durchsuche die masterMusicFiles, um die vollständigen Objekte zu finden
                for (String path : paths) {
                    for (MusicFiles masterFile : musicFiles) {
                        if (masterFile.getPath().equals(path)) {
                            reconstructedPlaylist.add(masterFile);
                            break;
                        }
                    }
                }
                currentPlaylist = reconstructedPlaylist;
            } else {
                // Fallback
                currentPlaylist = musicFiles;
            }
        }

        position = preferences.getInt("songIndex",-1);
        repeatBoolean = preferences.getBoolean("repeat", false);
        shuffleBoolean = preferences.getBoolean("shuffle", false);
        //VALIDIEREN
        if (currentPlaylist == null || currentPlaylist.isEmpty()) {
            currentPlaylist = musicFiles;
        }
        if (position < 0 || position >= currentPlaylist.size()) {
            position = 0;
        }

        //SONG LADEN (ohne Auto-Play)
        createMediaPlayer(position, false);
        createNotificationChannel();
        registerReceiver(becomingnoisyReceiver, becomingNoisyIntentFilter);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onAudioFocusChange(int focusState) {
        //Invoked when the audio focus of the system is updated.
        switch (focusState) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if(playWhenReady) {
                    start();
                    playWhenReady = false;
                }
                if(mediaPlayer != null) {
                    try {
                        mediaPlayer.setVolume(1.0f, 1.0f);
                    } catch (IllegalStateException ignored) {}
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                if(mediaPlayer != null) {
                    fadeOutAndPause();
                }
                playWhenReady = false;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if(mediaPlayer != null) {
                    if(isPlaying()){
                        pause();
                        playWhenReady = true;
                    }
                    fadeOutAndPause();
                }else playWhenReady = false;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // KURZE PAUSE, wenn Nachricht erscheint, Telefon klingelt,,,,
                if (isPlaying()) {
                    try {
                        mediaPlayer.setVolume(0.2f, 0.2f);
                    } catch (IllegalStateException ignored) {}
                }else{
                    playWhenReady = false;
                }
                break;
        }
    }

    public class MyBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Esel PlayerActivity: onStartCommand");
        ensurePlayerState();
        if(intent == null)return START_STICKY;
        // ← DEBUG: Alles loggen was reinkommt
        String debugAction = intent.getStringExtra("actionName");
        Log.d("MusicService_DEBUG", "onStartCommand - actionName: " + debugAction);
        Log.d("MusicService_DEBUG", "onStartCommand - intent.getAction(): " + intent.getAction());

        /*if(mediaPlayer != null&& isPlaying()){
            String action = intent.getStringExtra("actionName");
            if("RESTORE_LAST_SONG".equals(action)){
                return START_STICKY;
            }
        }*/

        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case ACTION_PLAY_PAUSE:
                    if (mediaPlayer == null) {
                        createMediaPlayer(position, false);
                    }
                    playPauseBtnClicked();
                    break;
                case ACTION_NEXT:
                    nextBtnClicked();
                    break;
                case ACTION_PREV:
                    previousBtnClicked();
                    break;
                    case ACTION_SHUFFLE:
                    shuffleBtnClicked();
                    break;
                case ACTION_REPEAT:
                    repeatBtnClicked();
                    break;
                case ACTION_OPEN:
                    if (mediaPlayer != null) {
                        openPlayerUI();
                        break;
                    }
                case ACTION_CANCEL:
                    getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE)
                            .edit()
                            .putBoolean(PLAYER_PLAYING, false)
                            .apply();

                    if (mediaPlayer != null) {
                        mediaPlayer.release();
                        mediaPlayer = null;
                    }
                    // NEU: Widget updaten VOR stopSelf()
                    sendWidgetUpdate();
                    // Notification entfernen
                    stopForeground(true);
                    // Service beenden
                    stopSelf();

                    break;
            }
            return START_STICKY;
        }

        // Logik für Bluetooth-Kommunikation
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            KeyEvent keyEvent = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                switch (keyEvent.getKeyCode()) {
                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    case KeyEvent.KEYCODE_MEDIA_PLAY:
                    case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    case KeyEvent.KEYCODE_HEADSETHOOK:
                        playPauseBtnClicked();
                        break;
                    case KeyEvent.KEYCODE_MEDIA_NEXT:
                        nextBtnClicked();
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                        previousBtnClicked();
                        break;
                }
            }
            return START_STICKY; // Befehl wurde verarbeitet
        }

        String actionName = intent.getStringExtra("actionName");
        if (actionName != null) {
            SharedPreferences.Editor editor = getSharedPreferences(MUSIC_LAST_PLAYED,MODE_PRIVATE).edit();
            switch (actionName) {
                case "WIDGET_CLICKED":
                    // Wenn ein Song geladen ist: PlayerActivity öffnen
                    if (mediaPlayer != null && position != -1) {
                        showNotification(R.drawable.ic_notif_play);

                        Intent openplayerActivity = new Intent(this, PlayerActivity.class);
                        openplayerActivity.putExtra("sender", "widget");
                        openplayerActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(openplayerActivity);
                    }
                    // Sonst: PlayerActivity öffnen, Song wird aus SharedPreferences geladen, faLls vorhanden
                    else {
                        SharedPreferences prefs = getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE);
                        int lastPosition = prefs.getInt("currentPosition", -1);

                        if (lastPosition != -1 && currentPlaylist != null && lastPosition < currentPlaylist.size()) {
                            createMediaPlayer(lastPosition, false);  // Laden, aber nicht abspielen
                            showNotification(R.drawable.ic_notif_pause);
                            sendWidgetUpdate();

                            // PlayerActivity öffnen
                            Intent openPlayerActivity = new Intent(this, PlayerActivity.class);
                            openPlayerActivity.putExtra("targetFragment", "SongsFragment");
                            openPlayerActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(openPlayerActivity);
                        }else{// kein Song in SharedPreferences vorhanden, es wird die Songsliste auf der MainActivity geöffnet
                            Intent mainIntent = new Intent(this, MainActivity.class);
                            mainIntent.putExtra("targetFragment","SongsFragment");
                            mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(mainIntent);
                        }
                    }
                    break;
                case "RESTORE_LAST_SONG":
                    if (mediaPlayer != null) {
                        Log.d(TAG, "RESTORE ignoriert, Player existiert bereits");
                        break;
                    }
                    SharedPreferences prefs = getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE);
                    int lastPosition = prefs.getInt("currentPosition", -1);

                    if (lastPosition != -1 && currentPlaylist != null && lastPosition < currentPlaylist.size()) {
                        // MediaPlayer erstellen, aber NICHT abspielen
                        createMediaPlayer(lastPosition);
                        // Notification anzeigen (mit Pause-Icon, da nicht abspielend)
                        showNotification(R.drawable.ic_notif_pause);
                        sendWidgetUpdate();
                    }
                    break;
                case "PLAY_MASTER_SONG":
                    int masterPosition = intent.getIntExtra("position", -1);
                    if (masterPosition != -1) {
                        playNewPlayList(this.musicFiles,masterPosition);
                    }
                    break;
                case "PLAY_ALBUM_SONG":
                    ArrayList<MusicFiles> albumSongs = intent.getParcelableArrayListExtra("albumSongs");
                    int songPosition = intent.getIntExtra("position", -1);
                    if (albumSongs != null && songPosition != -1){
                        currentPlaylist = albumSongs;
                        playNewPlayList(albumSongs, songPosition);
                    }
                    break;
                case "PLAY_ARTIST_SONG":
                    ArrayList<MusicFiles> artistSongs = intent.getParcelableArrayListExtra("artistSongs");
                    int artistPosition = intent.getIntExtra("position", -1);
                    Log.d("MusicService", "PLAY_ARTIST_SONG: artistSongs=" + (artistSongs != null ? artistSongs.size() : "NULL") +
                            ", position=" + artistPosition);
                    if (artistSongs != null && artistPosition != -1){
                        currentPlaylist = artistSongs;
                        playNewPlayList(artistSongs, artistPosition);
                    }
                    break;
                case "previous":
                    previousBtnClicked();
                    break;
                case "play":
                    if (!isPlaying()) start();
                    break;
                case "pause":
                    if (isPlaying()) pause();
                    break;
                case "playPause":
                    playPauseBtnClicked();
                    break;
                case "next":
                    nextBtnClicked();
                    break;
                case "cancel":
                    Log.d(TAG, "Cancel-Aktion empfangen. Beende den Service.");
                    //Status auf false setzen VOR dem Stoppen
                    getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE)
                            .edit()
                            .putBoolean(PLAYER_PLAYING, false)
                            .apply();
                    //Widget updaten VOR stopSelf()
                    sendWidgetUpdate();
                    resetPlayer();

                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        if (audioManager != null) {
                            audioManager.abandonAudioFocusRequest(audioFocusRequest);
                        }
                    } else {
                        if (audioManager != null) {
                            audioManager.abandonAudioFocus(this);
                        }
                    }

                    // 3. Entferne die Benachrichtigung und beende den Vordergrund-Status
                    stopForeground(true);

                    // 4. Beende den Service selbst
                    stopSelf();

                    // 5. Informiere die offene MainActivity, damit der Mini-Player verschwindet
                    if (mainActivityCallback != null) {
                        mainActivityCallback.onPlaybackStateChanged();
                    }
                    // Wir kehren hier zurück, da der Service beendet wird.
                    return START_NOT_STICKY;
                case "shuffle":
                    shuffleBoolean = !shuffleBoolean;
                    editor.putBoolean("shuffle", shuffleBoolean);
                    editor.apply();
                    break;
                case "repeat":
                    repeatBoolean = !repeatBoolean;
                    editor.putBoolean("repeat", repeatBoolean);
                    editor.apply();
                    break;
            }
        }
        return START_STICKY;
    }

    private void updateMediaSessionState(){
        if(mediaSessionCompat == null || currentPlaylist == null || position < 0 || position >= currentPlaylist.size()){
            return;
        }
        //Import der Metadaten des laufenden Songs
        MusicFiles currentSong = currentPlaylist.get(position);
        byte[] albumart = getAlbumArt(currentSong.getPath());
        Bitmap art;
        if(albumart != null){
            art = BitmapFactory.decodeByteArray(albumart, 0, albumart.length);
        }else {
            art = BitmapFactory.decodeResource(getResources(), R.drawable.ic_notif_pause);
        }
        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentSong.getTitle())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentSong.getArtist())
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, art)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, getDuration());

        mediaSessionCompat.setMetadata(metadataBuilder.build());
        PlaybackStateCompat.Builder playbackStateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY
                                | PlaybackStateCompat.ACTION_PAUSE
                                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                                | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                                | PlaybackStateCompat.ACTION_PLAY_PAUSE
                                | PlaybackStateCompat.ACTION_STOP
                                | PlaybackStateCompat.ACTION_SEEK_TO
                );
        if(isPlaying()){
            playbackStateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, getCurrentPosition(), 1.0f);
        }else{
            playbackStateBuilder.setState(PlaybackStateCompat.STATE_PAUSED, getCurrentPosition(), 0f);
        }
        mediaSessionCompat.setPlaybackState(playbackStateBuilder.build());
        // Info an Smartphone: Session ist aktiv!
        mediaSessionCompat.setActive(true);
    }

    public void playMedia(int startPosition) {
        if (D) Log.d(TAG, "Esel MusicService: playMedia");
        if (currentPlaylist == null || currentPlaylist.isEmpty()) {
            Log.e(TAG, "Esel PlayerActivity: playMedia: Keine Songs in der musicFiles");
            return;
        }
        createMediaPlayer(startPosition,true);
    }

    private void resetPlayer() {
        if (mediaPlayer != null) {
            try {
                if (isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error stopping MediaPlayer in resetPlayer", e);
            }
            try {
                mediaPlayer.reset();
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing MediaPlayer", e);
            }
            mediaPlayer = null;
        }
        EqualizerManager.getInstance().release();
    }

    void createMediaPlayer(int position, boolean autoPlay) {
        if (position < 0 || currentPlaylist == null || position >= currentPlaylist.size()) {
            resetPlayer();
            return;
        }
        this.position = position;
        uri = Uri.parse(currentPlaylist.get(position).getPath());
        SharedPreferences.Editor editor = getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE).edit();
        editor.putInt("songIndex", position);
        editor.putString(MUSIC_FILE, uri.toString());
        editor.putString(ARTIST_NAME, currentPlaylist.get(position).getArtist());
        editor.putString(SONG_NAME, currentPlaylist.get(position).getTitle());
        try {
            long albumId = Long.parseLong(currentPlaylist.get(position).getAlbumId());
            editor.putLong(ALBUM_ID, albumId);
        } catch (NumberFormatException e) {
            editor.putLong(ALBUM_ID, -1L);
            Log.e(TAG, "Album-ID konnte nicht als Long gespeichert werden: " + currentPlaylist.get(position).getAlbumId());
        }
        editor.apply();

        resetPlayer();

        try {
            Uri uri = Uri.parse(currentPlaylist.get(position).getPath());
            mediaPlayer = MediaPlayer.create(getApplicationContext(), uri);
            if(mediaPlayer != null) {
                int audioSessionId = mediaPlayer.getAudioSessionId();
                EqualizerManager.getInstance().initEqualizer(audioSessionId);

                mediaPlayer.setOnCompletionListener(this);
                saveCurrentSongToPrefs();
                updateMediaSessionState();

                if (autoPlay) {
                    start();
                }else{
                    showNotification(R.drawable.ic_notif_pause);
                }
            }
        } catch(Exception e) {
            Log.e(TAG, "Esel PlayerActivity: playMedia: MediaPlayer konnte nicht erstellt werden", e);
            mediaPlayer = null;
        }
    }

    // für Abwärtskompatibilität (startet automatisch)
    void createMediaPlayer(int position) {
        createMediaPlayer(position, true);
    }

    public void setCallBack(ActionPlaying actionPlaying) {
        this.actionPlaying = actionPlaying;
    }


    public void start() {
        Log.d(TAG, "Esel MusicService: start");
        // Sicherheitscheck
        if (mediaPlayer == null) {
            Log.e(TAG, "start: Versuch, einen nicht existierenden MediaPlayer zu starten.");
            return;
        }

        // Audio-Fokus anfordern
        int result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            result = audioManager.requestAudioFocus(audioFocusRequest);
        } else {
            result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            // Starte den Player
            try {
                mediaPlayer.start();
                if(currentPlaylist!= null && position >= 0 && position < currentPlaylist.size()){
                    String songId = currentPlaylist.get(position).getId();
                    PlaylistManager.getInstance(getApplicationContext()).addToRecent(songId);
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error starting mediaPlayer", e);
                return;
            }

            // 1. Speichere den neuen Zustand für Widget etc.
            SharedPreferences.Editor editor = getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE).edit();
            editor.putString(MUSIC_FILE, currentPlaylist.get(position).getPath());
            editor.putString(ARTIST_NAME, currentPlaylist.get(position).getArtist());
            editor.putString(SONG_NAME, currentPlaylist.get(position).getTitle());
            try {
                editor.putLong(ALBUM_ID, Long.parseLong(currentPlaylist.get(position).getAlbumId()));
            } catch (NumberFormatException ignored) {}
            editor.putBoolean(PLAYER_PLAYING, true);
            editor.putInt("songPosition",position);
            editor.putInt("songIndex",position);
            if(currentPlaylist != null && position >= 0 && position < currentPlaylist.size()){
                editor.putString("current_song_id", currentPlaylist.get(position).getId());
                Log.d("DEBUG","current_song_id: "+currentPlaylist.get(position).getId());
            }
            boolean isMasterPlaylist = (currentPlaylist == musicFiles);
            editor.putBoolean("isMasterPlaylist", isMasterPlaylist);
            if(!isMasterPlaylist){
                ArrayList<String> paths = new ArrayList<>();
                for (MusicFiles file : currentPlaylist) {
                    paths.add(file.getPath());
                }
                String jsonPaths = new Gson().toJson(paths);
                editor.putString("albumSongs", jsonPaths);
            }
            editor.apply();
            if(mainActivityCallback != null){
                mainActivityCallback.onPlaybackStateChanged();
            }
            if(actionPlaying != null){
                actionPlaying.updatePlayerView();
            }
            Intent updateIntent = new Intent("SONG_CHANGED");
            updateIntent.putExtra("songId", currentPlaylist.get(position).getId());
            sendBroadcast(updateIntent);

            // 2. Informiere die externen UIs (Notification & Widget)
            showNotification(R.drawable.ic_notif_play); // Musik spielt -> zeige PAUSE-Icon
            sendWidgetUpdate();

            // 3. Informiere das Android-System
            updateMediaSessionState();

            // --- HIER IST DIE WIEDERHERGESTELLTE, ENTSCHEIDENDE LOGIK ---
            // 4. Informiere die internen, offenen UIs (PlayerActivity & MainActivity)
            if (actionPlaying != null) {
                // Sagt der PlayerActivity, dass sich der Play/Pause-Status geändert hat
                actionPlaying.updatePlayerView();
            }
            if (mainActivityCallback != null) {
                // Sagt der MainActivity, dass sich der Play/Pause-Status geändert hat
                mainActivityCallback.onPlaybackStateChanged();
            }
            // -----------------------------------------------------------------
        } else {
            Log.e(TAG, "start: AudioFocusRequest fehlgeschlagen.");
        }
    }


    void pause() {
        if (mediaPlayer == null || !isPlaying()) {
            return;
        }
        Log.d(TAG, "Pause-Befehl empfangen. Player wird pausiert, UI wird aktualisiert.");

        try {
            int currentPos = mediaPlayer.getCurrentPosition();
            SharedPreferences.Editor editor = getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE).edit();
            editor.putInt("currentPosition", currentPos);
            editor.putBoolean(PLAYER_PLAYING, false);
            editor.apply();
        } catch (IllegalStateException ignored) {}

        showNotification(R.drawable.ic_notif_pause);
        sendWidgetUpdate();

        if (actionPlaying != null) {
            actionPlaying.updatePlayPauseButton();
        }
        if(mainActivityCallback != null){
            mainActivityCallback.onPlaybackStateChanged();
        }
        try {
            mediaPlayer.pause();
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error pausing mediaPlayer", e);
        }
        updateMediaSessionState();
    }

    public boolean isPlaying() {
        try {
            return mediaPlayer != null && mediaPlayer.isPlaying();
        } catch (IllegalStateException e) {
            Log.e(TAG, "IllegalStateException in isPlaying()", e);
            return false;
        }
    }

    void stop() {
        try {
            if (mediaPlayer != null && isPlaying()) {
                mediaPlayer.stop();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in stop()", e);
        }
        cancelNotification();
    }

    void release() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
            }
        } else {
            audioManager.abandonAudioFocus(this);
        }
        resetPlayer();
    }

    int getDuration() {
        if(mediaPlayer == null) return 0;
        try {
            return mediaPlayer.getDuration();
        } catch (IllegalStateException e) {
            return 0;
        }
    }


    public int showCurrentPosition() {
        Log.d(TAG, "Esel PlayerActivity: showCurrentPosition");
        SharedPreferences preferences = getSharedPreferences(MUSIC_LAST_PLAYED, Context.MODE_PRIVATE);
        return preferences.getInt("currentPosition", 0);
    }

    public int getCurrentPosition() {
        Log.d(TAG, "Esel PlayerActivity: getCurrentPosition");
        if(mediaPlayer == null) return 0;
        try {
            return mediaPlayer.getCurrentPosition();
        } catch (IllegalStateException e) {
            return 0;
        }
    }

    void seekTo(int seekbarpositioninMillis) {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.seekTo(seekbarpositioninMillis);
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error in seekTo", e);
            }
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if(D)Log.d(TAG, "Esel PlayerActivity: onCompletion, Song ist zu Ende");
        this.nextBtnClicked();
    }

    public void showNotification(int playPauseBtn) {
        if (D) Log.d(TAG, "Esel PlayerActivity: showNotification");
        if (position < 0 || currentPlaylist == null || position >= currentPlaylist.size()) {
            Log.e(TAG, "showNotification: Ungültige Position, kann keine Benachrichtigung erstellen.");
            return;
        }

        final int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        Intent contentIntent = new Intent(getApplicationContext(), PlayerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 5,
                contentIntent, flags);

        Intent prevIntent = new Intent(this, MusicService.class).setAction(ACTION_PREV);
        PendingIntent prevPendingIntent = PendingIntent.getService(this, 1,
                prevIntent, flags);

        Intent playIntent = new Intent(this, MusicService.class).setAction(ACTION_PLAY_PAUSE);
        PendingIntent playPendingIntent = PendingIntent.getService(this, 2,
                playIntent, flags);

        Intent nextIntent = new Intent(this, MusicService.class).setAction(ACTION_NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getService(this, 3,
                nextIntent, flags);


        Intent deleteIntent = new Intent(this, MusicService.class).setAction(ACTION_CANCEL);
        PendingIntent deletePendingIntent = PendingIntent.getService(this, 4,
                deleteIntent, flags);

        byte[] picture;
        picture = getAlbumArt(currentPlaylist.get(position).getPath());
        Bitmap thumb;
        if (picture != null && picture.length >0) {
            thumb = BitmapFactory.decodeByteArray(picture, 0, picture.length);
        } else {
            thumb = BitmapFactory.decodeResource(getResources(), R.drawable.ic_notif_pause);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_2)
                .setContentIntent(pendingIntent)
                .setSmallIcon(playPauseBtn)
                .setLargeIcon(thumb)
                .setContentTitle(currentPlaylist.get(position).getTitle())
                .setContentText(currentPlaylist.get(position).getArtist())
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .addAction(R.drawable.ic_notif_backward,"Prev",prevPendingIntent)
                .addAction(playPauseBtn,"Play",playPendingIntent)
                .addAction(R.drawable.ic_notif_forward,"Next",nextPendingIntent)
                .addAction(R.drawable.ic_notif_remove,"Close",deletePendingIntent)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSessionCompat.getSessionToken())
                        .setShowActionsInCompactView(0,1,2));

        Notification notification = builder.build();
        if (ActivityCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        startForeground(noteId,notification);
    }

    private byte[] getAlbumArt(String uri) {
        if (D) Log.d(TAG, "Esel PlayerActivity: getAlbumArt");
        if(uri == null) return null;
        try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
            retriever.setDataSource(uri);
            return retriever.getEmbeddedPicture();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get album art", e);
        }
        return null;
    }

    public void cancelNotification() {
        if(D)Log.d(TAG, "Esel PlayerActivity: killNotification");
        notificationManager.cancel(noteId);
    }

    public void playPauseBtnClicked() {
        if(mediaPlayer == null) {
            // Falls kein Player da ist (z.B. nach Restore), lade den letzten Song
            SharedPreferences prefs = getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE);
            int lastPosition = prefs.getInt("currentPosition", -1);
            if (lastPosition != -1) {
                createMediaPlayer(lastPosition);
            }
            return;
        }
        if(isPlaying()){
            pause();
        } else {
            start();
        }
        if(actionPlaying != null) {
            actionPlaying.updatePlayPauseButton();
        }
        if(mainActivityCallback != null){
            mainActivityCallback.onPlaybackStateChanged();
        }
        updateMediaSessionState();
        sendWidgetUpdate();
    }


    void previousBtnClicked() {
        if (repeatBoolean) {
            // Position bleibt gleich
        } else if (shuffleBoolean) {
            position = new Random().nextInt(currentPlaylist.size());
        } else {
            position = (position - 1 < 0) ? (currentPlaylist.size() - 1) : (position - 1);
        }

        createMediaPlayer(position);
        
        // Informiere die Activity, dass sie ihre UI aktualisieren muss
        if (actionPlaying != null) {
            actionPlaying.prevBtnClicked(); // Veranlasst die PlayerActivity, ihre UI zu aktualisieren.
            actionPlaying.updatePlayPauseButton();
        }
        if(mainActivityCallback != null){
            mainActivityCallback.onSongChanged();
            mainActivityCallback.onPlaybackStateChanged();
        }
        sendWidgetUpdate();
        if (miniPlayerCallback != null) miniPlayerCallback.onSongChanged();
    }

    void nextBtnClicked() {
        if (D) Log.d(TAG, "Esel PlayerActivity: nextBtnClicked");
        if(currentPlaylist == null || currentPlaylist.isEmpty()){return;}

        if (repeatBoolean) {
            // Position bleibt gleich, nichts zu tun
        } else if (shuffleBoolean) {
            position = new Random().nextInt(currentPlaylist.size());
        } else {
            position = (position + 1) % currentPlaylist.size();
        }

        createMediaPlayer(position);

        if (actionPlaying != null) {
            // Dies veranlasst die PlayerActivity, ihre updateUiComponents() aufzurufen.
            actionPlaying.nextBtnClicked();
            actionPlaying.updatePlayPauseButton();
        }

        if (mainActivityCallback != null) {
            mainActivityCallback.onSongChanged();
            mainActivityCallback.onPlaybackStateChanged();
        }

        sendWidgetUpdate();
        if (miniPlayerCallback != null) miniPlayerCallback.onSongChanged();
    }


    @Override
    public void onDestroy() {
        cancelNotification();
        stopForeground(true);
        try {
            unregisterReceiver(becomingnoisyReceiver);
        } catch (Exception ignored) {}
        release();
        if(mediaSessionCompat != null){
            mediaSessionCompat.setActive(false);
            mediaSessionCompat.release();
            mediaSessionCompat = null;
        }
        super.onDestroy();
    }

    private void createNotificationChannel() {
        int importance = NotificationManager.IMPORTANCE_LOW;
        CharSequence name = "Berlin Music Player";
        String description = "Berlin Music Player";
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID_2, name, importance);
        channel.setDescription(description);
        notificationManager.createNotificationChannel(channel);
    }

    public void setCurrentPosition(){
        Log.d(TAG, "Esel PlayerActivity: setCurrentPosition");
        if (mediaPlayer == null) return;
        SharedPreferences preferences = getSharedPreferences(MUSIC_LAST_PLAYED, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        try {
            int currentPos = mediaPlayer.getCurrentPosition();
            editor.putInt("currentPosition", currentPos);
            editor.apply();
        } catch (IllegalStateException ignored) {}
    }
    
    void resume() {
        Log.d(TAG, "Esel PlayerActivity: resume");
        if (mediaPlayer == null) return;
        int pausedPosition = getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE).getInt("pausedPosition", 0);
        try {
            mediaPlayer.seekTo(pausedPosition);
            mediaPlayer.start();
        } catch (IllegalStateException ignored) {}
    }

    public void setMainActivityCallback(MusicServiceCallback callback) {
        this.mainActivityCallback = callback;
    }

    public void shuffleAll() {
        if (currentPlaylist == null || currentPlaylist.isEmpty()) {
            return;
        }
        shuffleBoolean = true;
        repeatBoolean = false;
        currentPlaylist = musicFiles;
        position = new Random().nextInt(currentPlaylist.size());

        createMediaPlayer(position);

        if (mainActivityCallback != null) {
            mainActivityCallback.onSongChanged();
        }
        sendWidgetUpdate();

        Intent intent = new Intent(this, PlayerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public void setSongList(ArrayList<MusicFiles> songListFromMainActivity) {
        this.currentPlaylist = new ArrayList<>(songListFromMainActivity);
        Log.d(TAG, "Esel MusicService: MusicService hat jetzt eine Songliste mit " + songListFromMainActivity.size() + " Songs");
    }



    public ArrayList<MusicFiles> getAllAudio() {
        SharedPreferences preferences = getSharedPreferences(MainActivity.MY_SORT_PREF, MODE_PRIVATE);
        String sortOrder = preferences.getString("sorting", "sortByName");
        ArrayList<MusicFiles> tempAudioList = new ArrayList<>();
        String order;
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        switch (sortOrder) {
            case "sortByDate":
                order = MediaStore.MediaColumns.DATE_ADDED + " DESC";
                break;
            case "sortBySize":
                order = MediaStore.MediaColumns.SIZE + " DESC";
                break;
            case "sortByName":
            default:
                order = MediaStore.MediaColumns.TITLE + " COLLATE NOCASE ASC";
                break;
        }
        String[] projection = {
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.YEAR,
                //MediaStore.Audio.Media.GENRE,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.SIZE
        };
        try (Cursor cursor = getContentResolver().query(uri, projection, null, null, order)) {
            if (cursor != null) {
                int albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                int titleIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int durationIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                int pathIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                int artistIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int idIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int albumIdIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
                int albumYearIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR);
                //int albumGenreIdx = cursor.getColumnIndex(MediaStore.Audio.Media.GENRE);
                int dateAddedIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED);
                int sizeIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);

                while (cursor.moveToNext()) {
                    String album = cursor.getString(albumColumn);
                    String title = cursor.getString(titleIdx);
                    String duration = cursor.getString(durationIdx);
                    String path = cursor.getString(pathIdx);
                    String artist = cursor.getString(artistIdx);
                    String id = cursor.getString(idIdx);
                    String albumId = cursor.getString(albumIdIdx);
                    String albumYear = cursor.getString(albumYearIdx);
                    //String albumgenre = cursor.getString(albumGenreIdx);
                    long dateAdded = cursor.getLong(dateAddedIdx);
                    long size = cursor.getLong(sizeIdx);


                    MusicFiles musicFile = new MusicFiles(path, title, artist, album, duration, id, albumId,albumYear, dateAdded, size);
                    tempAudioList.add(musicFile);
                }
            }
        }
        return tempAudioList;
    }

    public void shuffleBtnClicked(){
        shuffleBoolean = !shuffleBoolean;
        SharedPreferences.Editor editor = getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE).edit();
        editor.putBoolean("shuffle", shuffleBoolean);
        editor.apply();

        // 3. UI informieren (Activity und Widget)
        if (actionPlaying != null) {
            actionPlaying.updatePlayerView();
            actionPlaying.updateShuffleRepeatButtons();
        }
        sendWidgetUpdate();
    }
    public void repeatBtnClicked() {
        repeatBoolean = !repeatBoolean;
        SharedPreferences.Editor editor = getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE).edit();
        editor.putBoolean("repeat", repeatBoolean);
        editor.apply();

        // 3. UI informieren
        if (actionPlaying!= null) {
            actionPlaying.updatePlayerView();
            actionPlaying.updateShuffleRepeatButtons();
        }
        sendWidgetUpdate();
    }

    private void saveCurrentSongToPrefs() {
        if (position < 0 || currentPlaylist == null || position >= currentPlaylist.size()) return;
        SharedPreferences.Editor editor = getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE).edit();
        editor.putString(MUSIC_FILE, currentPlaylist.get(position).getPath());
        editor.putString(ARTIST_NAME, currentPlaylist.get(position).getArtist());
        editor.putString(SONG_NAME, currentPlaylist.get(position).getTitle());
        try {
            long albumId = Long.parseLong(currentPlaylist.get(position).getAlbumId());
            editor.putLong(ALBUM_ID, albumId);
        } catch (NumberFormatException e) {
            editor.putLong(ALBUM_ID, -1L);
        }
        editor.apply();
    }

    private void fadeOutAndPause() {
        if (mediaPlayer == null || !isPlaying()) {
            return; // Nichts zu tun, wenn nichts spielt
        }

        // ValueAnimator, der von 1.0f (volle Lautstärke) auf 0.0f (stumm) animiert
        final ValueAnimator volumeAnimator = ValueAnimator.ofFloat(1.0f, 0.0f);
        volumeAnimator.setDuration(2000); // Dauer des Ausblendens in Millisekunden (z.B. 800ms)

        volumeAnimator.addUpdateListener(animation -> {
            if (mediaPlayer != null) {
                float volume = (float) animation.getAnimatedValue();
                try {
                    mediaPlayer.setVolume(volume, volume);
                } catch (IllegalStateException ignored) {}
            }
        });

        volumeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Wenn die Animation beendet ist (Lautstärke ist 0), pausiere den Player
                if (isPlaying()) {
                    pause(); // Rufe deine normale pause()-Methode auf
                }
                release();
            }
        });
        volumeAnimator.start(); // Starte die Animation
    }

    public int getPosition(){
        return this.position;
    }

    public void playNewPlayList(ArrayList<MusicFiles> newPlayList, int startPosition){
        this.currentPlaylist = newPlayList;
        if(startPosition>=0 && startPosition<this.currentPlaylist.size()){
            if(actionPlaying != null) {
                actionPlaying.updatePlayerView();
            }
            playMedia(startPosition);
        }else{
            playMedia(0);
        }
    }
    public String getCurrentSongId(){
        if(currentPlaylist == null) return null;
        if(position < 0 || position >= currentPlaylist.size()) return null;
        return currentPlaylist.get(position).getId();
    }

    public ArrayList<MusicFiles> getMusicFiles(){
        return musicFiles;
    }

    public boolean getShuffleState(){
        return shuffleBoolean;
    }
    public boolean getRepeatState() {
        return repeatBoolean;
    }

    private void sendWidgetUpdate() {
        if(currentPlaylist == null || currentPlaylist.isEmpty() || position < 0) {return;}

        Intent intent = new Intent(this, MusicWidgetProvider.class);
        intent.setAction(ACTION_UPDATE_WIDGET);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, MusicWidgetProvider.class));
        if (appWidgetIds != null && appWidgetIds.length > 0) {
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
            sendBroadcast(intent);
        }
    }

    public interface MiniPlayerCallback{
        void onSongChanged();
    }

    public void setMiniPlayerCallback(MiniPlayerCallback callback) {
        this.miniPlayerCallback = callback;
    }

    public ArrayList<MusicFiles> getLibrary(){
        return musicFiles;
    }

    public void sortCurrentPlaylist(String sortOrder) {
        if (currentPlaylist == null || currentPlaylist.isEmpty()) return;

        // Speichere die neue Sortier-Präferenz
        SharedPreferences.Editor editor = getSharedPreferences(MainActivity.MY_SORT_PREF, MODE_PRIVATE).edit();
        editor.putString("sorting", sortOrder);
        editor.apply();

        // Sortiere die AKTUELLE Playlist
        switch (sortOrder) {
            case "sortByDate":
                Collections.sort(currentPlaylist, (a, b) -> Long.compare(Long.parseLong(b.getId()), Long.parseLong(a.getId())));
                break;
            case "sortBySize":
                // Logik zum Sortieren nach Größe (benötigt Größen-Info in MusicFiles)
                break;
            case "sortByName":
            default:
                Collections.sort(currentPlaylist, (a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()));
                break;
        }

        if (actionPlaying != null) {
            actionPlaying.updatePlayerView();
        }
    }

    public ArrayList<MusicFiles> getCurrentPlaylist() {
        return currentPlaylist != null ? currentPlaylist : musicFiles;
    }

    private void ensurePlayerState() {
        if (musicFiles == null || musicFiles.isEmpty()) {
            musicFiles = getAllAudio();
        }

        if (currentPlaylist == null || currentPlaylist.isEmpty()) {
            currentPlaylist = musicFiles;
        }

        if (position < 0 || position >= currentPlaylist.size()) {
            position = 0;
        }
    }
    private void openPlayerUI() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }


    public void setMusicFiles(ArrayList<MusicFiles> newList) {
        if (newList != null && !newList.isEmpty()) {
            this.musicFiles = newList;
            // Position zurücksetzen falls außerhalb der neuen Liste
            if (position >= newList.size()) {
                position = 0;
            }
        }
    }
    public EqualizerManager getEqualizer(){
        return EqualizerManager.getInstance();
    }

}
