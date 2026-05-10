package com.berlinmusicplayer;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ArtistDetails extends AppCompatActivity {
    RecyclerView recyclerView;
    ImageView artistPhoto;
    String artistName;
    ArrayList<MusicFiles> artistSongs = new ArrayList<>();
    ArtistDetailAdapter artistDetailAdapter;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_details);

        recyclerView = findViewById(R.id.artist_recyclerview);
        artistPhoto = findViewById(R.id.artistPhoto);
        artistName = getIntent().getStringExtra("artistName");

    }
}
