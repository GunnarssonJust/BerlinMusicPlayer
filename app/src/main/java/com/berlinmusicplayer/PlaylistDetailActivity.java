package com.berlinmusicplayer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Locale;

public class PlaylistDetailActivity extends AppCompatActivity {

    private int playlistIndex;
    private String playlistName;
    private PlaylistManager playlistManager;
    private ArrayList<MusicFiles> songs;
    private ArtistDetailAdapter adapter;  // Wiederverwendung des vorhandenen Adapters!
    private RecyclerView recyclerView;
    private TextView emptyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        playlistIndex = getIntent().getIntExtra("playlistIndex", -1);
        playlistName = getIntent().getStringExtra("playlistName");
        playlistManager = PlaylistManager.getInstance(this);

        TextView title = findViewById(R.id.playlist_detail_title);
        recyclerView = findViewById(R.id.playlist_detail_recyclerView);
        emptyText = findViewById(R.id.playlist_detail_empty);

        title.setText(playlistName);

        songs = playlistManager.getSongsForPlaylist(playlistIndex);
        songs.sort((a,b)->
                a.getTitle().compareToIgnoreCase(b.getTitle()));
        updateUI();

        adapter = new ArtistDetailAdapter(this, songs,playlistIndex);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        songs = playlistManager.getSongsForPlaylist(playlistIndex);
        if (adapter != null) adapter.updateList(songs);
        updateUI();
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
}
