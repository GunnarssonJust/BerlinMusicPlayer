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
import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

public class NowPlayingFragmentBottom extends Fragment {

    private static final String TAG = "NowPlayingFragment";

    private ImageView albumArt, nextBtn, playPauseBtn;
    private TextView artist, songName;
    private BaseActivity hostActivity;

    // Lokale Variablen, um den Zustand zu halten, auch wenn der Service (noch) nicht läuft
    private String lastPlayedPath;
    private boolean isPlayingState = false;

    public NowPlayingFragmentBottom() {
        // Erforderlicher leerer öffentlicher Konstruktor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // ← Nicht mehr nur MainActivity!
        if (context instanceof BaseActivity) {
            hostActivity = (BaseActivity) context;
        } else if (context instanceof MainActivity) {
            hostActivity = (MainActivity) context;
        } else {
            throw new RuntimeException(context.toString() + " muss BaseActivity sein");
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

    MusicService getMusicService() {
        if (hostActivity != null) {
            return hostActivity.musicService;
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
            Intent intent = new Intent(hostActivity, PlayerActivity.class);
            intent.putExtra("sender", "NowPlayingFragment");
            hostActivity.startActivity(intent);
        } else if (lastPlayedPath != null) {
            // Wenn der Service NICHT läuft, aber ein letzter Song bekannt ist -> Starte den Player mit diesem Song
            int position = findPositionByPath(lastPlayedPath);
            if (position != -1) {
                Intent intent = new Intent(hostActivity, PlayerActivity.class);
                intent.putExtra("position", position);
                hostActivity.startActivity(intent);
            }
        }
    }

    // --- HILFSMETHODEN, UM DIE LOGIK ABSTURZSICHER ZU MACHEN ---

    private void startLastPlayedSong() {
        if (hostActivity != null && lastPlayedPath != null) {
            int position = findPositionByPath(lastPlayedPath);
            if (position != -1) {
                Intent serviceIntent = new Intent(hostActivity, MusicService.class);
                serviceIntent.putExtra("servicePosition", position);
                hostActivity.startService(serviceIntent);

                // UI sofort aktualisieren für besseres Feedback
                playPauseBtn.setImageResource(R.mipmap.ic_pause);
                this.isPlayingState = true;
            }
        }
    }

    // --- FINALE, KORREKTE UI-UPDATE-METHODE ---

    public void updateNowPlayingUI(MusicFiles musicFile, boolean isPlaying) {
        if(!isAdded() || getContext() == null) return;
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
                    Glide.with(this).asBitmap().load(R.drawable.ic_play_btn).into(albumArt);
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
        if(hostActivity == null) return -1;
        return hostActivity.findSongPositionByPath(path);
    }

    @Override
    public void onResume() {
        super.onResume();
        MusicService s = getMusicService();
        if (s == null) return;
        java.util.ArrayList<MusicFiles> list = s.getCurrentPlaylist();
        int pos = s.getPosition();
        if (list != null && pos >= 0 && pos < list.size()) {
            updateNowPlayingUI(list.get(pos), s.isPlaying());
        }
    }

}
