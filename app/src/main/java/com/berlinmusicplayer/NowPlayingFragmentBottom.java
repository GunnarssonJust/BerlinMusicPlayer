package com.berlinmusicplayer;

import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class NowPlayingFragmentBottom extends Fragment {

    private static final String TAG = "NowPlayingFragment";

    private ImageView albumArt, nextBtn, playPauseBtn;
    private TextView artist, songName;
    private MainActivity mainActivity;

    // Lokale Variablen, um den Zustand zu halten, auch wenn der Service (noch) nicht läuft
    private String lastPlayedPath;
    private boolean isPlayingState = false;

    public NowPlayingFragmentBottom() {
        // Erforderlicher leerer öffentlicher Konstruktor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " muss MainActivity sein");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_now_playing_bottom, container, false);

        // Initialisiere die Views
        artist = view.findViewById(R.id.song_artist_miniPlayer);
        songName = view.findViewById(R.id.song_name_miniPlayer);
        albumArt = view.findViewById(R.id.bottom_album_art);
        nextBtn = view.findViewById(R.id.skip_next_bottom);
        playPauseBtn = view.findViewById(R.id.play_pause_miniPlayer);
        songName.setSelected(true);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        playPauseBtn.setOnClickListener(v -> handlePlayPauseClick());
        nextBtn.setOnClickListener(v -> handleNextClick());
        view.setOnClickListener(v -> handleFragmentClick());
    }

    // Hilfsmethode, um sicher auf den MusicService zuzugreifen
    private MusicService getMusicService() {
        if (mainActivity != null) {
            return mainActivity.musicService;
        }
        return null;
    }

    // --- ROBUSTE CLICK-HANDLER ---

    private void handlePlayPauseClick() {
        Log.d(TAG, "handlePlayPauseClick aufgerufen");
        MusicService musicService = getMusicService();
        if (musicService != null && (musicService.isPlaying() || musicService.mediaPlayer != null)) {
            // Wenn der Service läuft (egal ob er spielt oder nur pausiert ist) -> sende Befehl
            musicService.playPauseBtnClicked();
        } else {
            // Wenn der Service NICHT läuft -> Starte die Wiedergabe mit dem zuletzt gespielten Song
            startLastPlayedSong();
        }
    }

    private void handleNextClick() {
        Log.d(TAG, "handleNextClick aufgerufen");
        MusicService musicService = getMusicService();
        if (musicService != null) {
            musicService.nextBtnClicked();
        }
    }

    private void handleFragmentClick() {
        Log.d(TAG, "handleFragmentClick aufgerufen");
        MusicService musicService = getMusicService();
        if (musicService != null && musicService.mediaPlayer != null) {
            // Wenn der Service läuft -> Öffne einfach den Player
            Intent intent = new Intent(mainActivity, PlayerActivity.class);
            intent.putExtra("sender", "NowPlayingFragment");
            mainActivity.startActivity(intent);
        } else if (lastPlayedPath != null) {
            // Wenn der Service NICHT läuft, aber ein letzter Song bekannt ist -> Starte den Player mit diesem Song
            int position = findPositionByPath(lastPlayedPath);
            if (position != -1) {
                Intent intent = new Intent(mainActivity, PlayerActivity.class);
                intent.putExtra("position", position);
                mainActivity.startActivity(intent);
            }
        }
    }

    // --- HILFSMETHODEN, UM DIE LOGIK ABSTURZSICHER ZU MACHEN ---

    private void startLastPlayedSong() {
        if (mainActivity != null && lastPlayedPath != null) {
            int position = findPositionByPath(lastPlayedPath);
            if (position != -1) {
                Intent serviceIntent = new Intent(mainActivity, MusicService.class);
                serviceIntent.putExtra("servicePosition", position);
                mainActivity.startService(serviceIntent);

                // UI sofort aktualisieren für besseres Feedback
                playPauseBtn.setImageResource(R.mipmap.ic_pause);
                this.isPlayingState = true;
            }
        }
    }

    // --- FINALE, KORREKTE UI-UPDATE-METHODE ---

    public void updateNowPlayingUI(MusicFiles musicFile, boolean isPlaying) {
        // Sicherheitscheck, falls die Views noch nicht initialisiert sind
        if (songName == null || artist == null || playPauseBtn == null) {
            return;
        }

        this.isPlayingState = isPlaying;

        if (musicFile != null) {
            // Fall 1: Wir haben Song-Informationen
            this.lastPlayedPath = musicFile.getPath();
            songName.setText(musicFile.getTitle());
            artist.setText(musicFile.getArtist());

            if (albumArt != null) {
                byte[] artBytes = getAlbumArt(musicFile.getPath());
                if (artBytes != null) {
                    Glide.with(requireContext()).asBitmap().load(artBytes).into(albumArt);
                } else {
                    Glide.with(requireContext()).asBitmap().load(R.drawable.ic_play_btn).into(albumArt);
                }
            }
        }
        // Fall 2: musicFile ist null. UI bleibt unverändert, aber stürzt nicht ab.

        // Aktualisiere den Play/Pause-Button in jedem Fall
        playPauseBtn.setImageResource(isPlaying ? R.mipmap.ic_pause : R.mipmap.ic_play_pressed);
    }

    private byte[] getAlbumArt(String uri) {
        if (uri == null) return null;
        try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
            retriever.setDataSource(uri);
            return retriever.getEmbeddedPicture();
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Laden des Album-Covers im Fragment", e);
        }
        return null;
    }
    private int findPositionByPath(String path){
        if(mainActivity == null) return -1;
        return mainActivity.findSongPositionByPath(path);
    }
}
