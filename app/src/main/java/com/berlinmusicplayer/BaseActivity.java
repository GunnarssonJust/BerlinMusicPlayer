package com.berlinmusicplayer;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public abstract class BaseActivity extends AppCompatActivity
        implements MusicServiceCallback {

    protected MusicService musicService;
    protected NowPlayingFragmentBottom bottomPlayer;

    private boolean serviceBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    // ---------------------------------------------------------
    // SERVICE VERBINDUNG
    // ---------------------------------------------------------

    protected final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MusicService.MyBinder binder =
                    (MusicService.MyBinder) iBinder;

            musicService = binder.getService();
            serviceBound = true;

            musicService.setMainActivityCallback(BaseActivity.this);

            onMusicServiceConnected();
            updateBottomPlayer();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
            serviceBound = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        // Mini-Player dieser Activity suchen
        bottomPlayer = (NowPlayingFragmentBottom)
                getSupportFragmentManager()
                        .findFragmentById(R.id.frag_bottom_player);

        // Service verbinden
        Intent intent = new Intent(this, MusicService.class);

        if (!serviceBound) {
            bindService(intent, serviceConnection, BIND_AUTO_CREATE);
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        if(musicService != null){
            musicService.setMainActivityCallback(null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (musicService != null) {
            musicService.setMainActivityCallback(this);
            updateBottomPlayer();
        }
    }

    @Override
    protected void onStop() {
        if (serviceBound) {
            try {
                unbindService(serviceConnection);
            } catch (Exception ignored) {
            }
            serviceBound = false;
        }
        super.onStop();
    }

    // ---------------------------------------------------------
    // MUSIC SERVICE
    // ---------------------------------------------------------

    public MusicService getMusicService() {
        return musicService;
    }

    // ---------------------------------------------------------
    // CALLBACKS VOM MUSIC SERVICE
    // ---------------------------------------------------------

    protected abstract void onMusicServiceConnected();

    @Override
    public void onSongChanged() {
        runOnUiThread(this::updateBottomPlayer);
    }

    @Override
    public void onPlaybackStateChanged() {
        runOnUiThread(this::updateBottomPlayer);
    }

    // ---------------------------------------------------------
    // MINI PLAYER AKTUALISIEREN
    // ---------------------------------------------------------

    protected void updateBottomPlayer() {
        if(isFinishing() || isDestroyed()) return;

        if (bottomPlayer == null || !bottomPlayer.isAdded()|| musicService == null) {
            return;
        }

        ArrayList<MusicFiles> playlist =
                musicService.getCurrentPlaylist();

        int position = musicService.getPosition();

        if (playlist != null
                && position >= 0
                && position < playlist.size()) {

            MusicFiles currentSong = playlist.get(position);

            bottomPlayer.updateNowPlayingUI(
                    currentSong,
                    musicService.isPlaying()
            );
        }
    }

    // ---------------------------------------------------------
    // SONG-POSITION
    // ---------------------------------------------------------

    public int findSongPositionByPath(String path) {

        if (musicService == null || path == null) {
            return -1;
        }

        ArrayList<MusicFiles> playlist =
                musicService.getMusicFiles();

        if (playlist == null) {
            return -1;
        }

        for (int i = 0; i < playlist.size(); i++) {

            MusicFiles song = playlist.get(i);

            if (song != null
                    && path.equals(song.getPath())) {

                return i;
            }
        }

        return -1;
    }
}