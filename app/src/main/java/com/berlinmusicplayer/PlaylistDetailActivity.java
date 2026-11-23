package com.berlinmusicplayer;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.SparseIntArray;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;

public class PlaylistDetailActivity extends BaseActivity {

    private int playlistIndex;
    private String playlistName;
    private PlaylistManager playlistManager;
    private ArrayList<MusicFiles> songs;
    private SongListAdapter adapter;  // Wiederverwendung des vorhandenen Adapters!
    private RecyclerView recyclerView;
    private TextView emptyText;
    private LinearLayout letterIndex;
    private TextView letterPopup;
    private Handler bubbleHandler;
    private SparseIntArray letterIndexMap = new SparseIntArray();
    private BroadcastReceiver songChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        getWindow().setStatusBarColor(getColor(R.color.darkestgreen));
        getWindow().setNavigationBarColor(getColor(R.color.darkestgreen));
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);

        playlistIndex = getIntent().getIntExtra("playlistIndex", -1);
        playlistName = getIntent().getStringExtra("playlistName");
        playlistManager = PlaylistManager.getInstance(this);

        Toolbar toolbar = findViewById(R.id.playlist_detail_toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        TextView title = findViewById(R.id.playlist_detail_title);
        recyclerView = findViewById(R.id.playlist_detail_recyclerView);
        emptyText = findViewById(R.id.playlist_detail_empty);

        title.setText(playlistName);

        songs = playlistManager.getSongsForPlaylist(playlistIndex);
        songs.sort((a,b)->
                a.getTitle().compareToIgnoreCase(b.getTitle()));
        updateUI();

        adapter = new SongListAdapter(this, songs,playlistIndex,true,true);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        letterIndex = findViewById(R.id.letter_index_playlist);
        letterPopup = findViewById(R.id.letter_popup_playlist);
        bubbleHandler = new Handler(Looper.getMainLooper());
        buildLetterIndex();
        setupLetterIndex();
    }

    @Override
    protected void onResume() {
        super.onResume();
        songs = playlistManager.getSongsForPlaylist(playlistIndex);
        if (adapter != null) adapter.updateList(songs);
        updateUI();
    }

    @Override
    protected void onStart() {
        super.onStart();
        songChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (adapter != null) adapter.notifyDataSetChanged();
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            registerReceiver(songChangeReceiver,
                    new IntentFilter("SONG_CHANGED"), RECEIVER_EXPORTED);
        } else {
            registerReceiver(songChangeReceiver, new IntentFilter("SONG_CHANGED"),RECEIVER_NOT_EXPORTED
                    );
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (songChangeReceiver != null) {
            unregisterReceiver(songChangeReceiver);
            songChangeReceiver = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        menu.add(0, 1, 0, "Sortieren")
                .setIcon(R.drawable.ic_gate)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(0, 2, 0, "Zufall")
                .setIcon(R.drawable.ic_notif_shuffle_24)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        if (item.getItemId() == 1){
            showSortDialog();
            return true;
        }
        if (item.getItemId() == 2){
            MusicService s = getMusicService();
            if (s != null) {
                s.shufflePlaylist(songs);
                startActivity(new Intent(this, PlayerActivity.class));
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void updateUI() {
        if (songs == null || songs.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
    private void showSortDialog() {
        String[] options = {"Nach Titel", "Nach Künstler"};
        new AlertDialog.Builder(this)
                .setTitle("Sortieren nach")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Collections.sort(songs, (a, b) ->
                                a.getTitle().compareToIgnoreCase(b.getTitle()));
                    } else {
                        Collections.sort(songs, (a, b) ->
                                a.getArtist().compareToIgnoreCase(b.getArtist()));
                    }
                    adapter.updateList(songs);
                    buildLetterIndex();
                    setupLetterIndex();
                    updateUI();
                })
                .show();
    }

    private void buildLetterIndex() {
        letterIndexMap.clear();
        for (int i = 0; i < songs.size(); i++) {
            char c = getFirstLetter(songs.get(i).getTitle());
            if (letterIndexMap.indexOfKey(c) < 0) {
                letterIndexMap.put(c, i);
            }
        }
    }

    private char getFirstLetter(String text) {
        if (text == null) return '#';
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) return Character.toUpperCase(c);
        }
        return '#';
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupLetterIndex() {
        letterIndex.removeAllViews();
        for (int i = 0; i < letterIndexMap.size(); i++) {
            char c = (char) letterIndexMap.keyAt(i);
            TextView tv = new TextView(this);
            tv.setText(String.valueOf(c));
            tv.setTextSize(9f);
            tv.setTextColor(getColor(R.color.white));
            tv.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
            tv.setLayoutParams(params);
            letterIndex.addView(tv);
        }

        letterIndex.setOnTouchListener((v, event) -> {
            float y = event.getY();
            int index = (int) (y / v.getHeight() * letterIndexMap.size());
            index = Math.max(0, Math.min(index, letterIndexMap.size() - 1));
            char c = (char) letterIndexMap.keyAt(index);
            int pos = letterIndexMap.get(c);

            recyclerView.scrollToPosition(pos);
            letterPopup.setText(String.valueOf(c));
            letterPopup.setVisibility(View.VISIBLE);

            bubbleHandler.removeCallbacksAndMessages(null);
            if (event.getAction() == MotionEvent.ACTION_UP) {
                bubbleHandler.postDelayed(() ->
                        letterPopup.setVisibility(View.GONE), 600);
            }
            return true;
        });
    }
    @Override
    protected void onMusicServiceConnected() {
        // z.B. leer lassen, falls ArtistDetails nichts extra braucht,
        // sobald der MusicService verbunden ist
    }


}
