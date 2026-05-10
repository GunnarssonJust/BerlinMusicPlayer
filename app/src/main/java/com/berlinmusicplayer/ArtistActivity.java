package com.berlinmusicplayer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;												  

import java.util.ArrayList;

public class ArtistActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    ImageView artistPhoto;
    TextView artistTitle;
    String artistName;
    ArrayList<MusicFiles> artistSongs = new ArrayList<>();
    ArtistPagerAdapter artistPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist);

        artistPhoto = findViewById(R.id.artistPhoto);
        artistTitle = findViewById(R.id.artistTitle);
		TabLayout tabLayout = findViewById(R.id.artist_tabLayout);
        ViewPager2 viewPager = findViewById(R.id.artist_viewPager);														  
        artistName = getIntent().getStringExtra("artistName");

        if (artistName == null || artistName.isEmpty()) {
            Log.e("ArtistActivity", "Fehler: artistName ist null!");
            finish();  // Activity sofort schließen
            return;
        }
        artistTitle.setText(artistName);

        artistSongs = getArtistSongs(this, artistName);
        Log.d("ArtistActivity", "Geladen: " + artistSongs.size() + " Songs für Artist: " + artistName);
        for (MusicFiles song : artistSongs) {
            Log.d("ArtistActivity", "Song: " + song.getTitle() + " - Album: " + song.getAlbum());
        }

        if (artistSongs.isEmpty()) {
            Log.e("ArtistActivity", "Fehler: artistSongs ist leer!");
            finish();
            return;
        }

        if (artistSongs.isEmpty()) {
            Log.e("ArtistActivity", "Fehler: artistSongs ist leer!");
            finish();  // Activity sofort schließen
            return;
        }


        // Cover des ersten Songs als Künstler-Bild laden

	
		byte[] image = getAlbumArt(artistSongs.get(0).getPath());
		if (image != null) {
			Glide.with(this).load(image).into(artistPhoto);
		} else {
			Glide.with(this).load(R.mipmap.ic_play).into(artistPhoto);
		}
        

        // ViewPager2 mit Adapter verknüpfen
        ArtistPagerAdapter pagerAdapter = new ArtistPagerAdapter(this, artistName, artistSongs);
        viewPager.setAdapter(pagerAdapter);

        // TabLayout mit ViewPager2 verknüpfen → Tab-Titel setzen
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText("Alben"); break;
                case 1: tab.setText("Titel"); break;
            }
        }).attach();
    }

    private byte[] getAlbumArt(String path) {
        try (android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever()) {
            retriever.setDataSource(path);
            return retriever.getEmbeddedPicture();
        } catch (Exception e) {
            return null;
        }
    }

    public ArrayList<MusicFiles> getArtistSongs(Context context, String artistName) {
        ArrayList<MusicFiles> songList = new ArrayList<>();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String selection = MediaStore.Audio.Media.ARTIST + "=?";
        String[] selectionArgs = new String[]{artistName};
        String sortOrder = MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC";

        String[] projection = {
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ALBUM_ID
        };

        try (Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String album = cursor.getString(0);
                    String title = cursor.getString(1);
                    String duration = cursor.getString(2);
                    String path = cursor.getString(3);
                    String artist = cursor.getString(4);
                    String id = cursor.getString(5);
                    String albumId = cursor.getString(6);
                    songList.add(new MusicFiles(path, title, artist, album, duration, id, albumId));
                }
            }
        }
        return songList;
    }
}