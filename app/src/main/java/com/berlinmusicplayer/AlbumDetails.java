package com.berlinmusicplayer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.util.ArrayList;

public class AlbumDetails extends AppCompatActivity {
    RecyclerView recyclerView;
    ImageView albumPhoto;
    String albumName;
    TextView album_Name,album_artist, albumYear;
    int songID;
    ArrayList<MusicFiles> albumSongs = new ArrayList<>();
    AlbumDetailsAdapter albumDetailsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_details);

        recyclerView = findViewById(R.id.album_recyclerView);
        albumPhoto = findViewById(R.id.albumPhoto);
        albumName = getIntent().getStringExtra("albumName");
        album_Name = findViewById(R.id.album_name);
        album_artist = findViewById(R.id.artist);
        albumYear = findViewById(R.id.album_details_year);

        albumSongs = getAlbumSongs(this, albumName);

        if(!albumSongs.isEmpty()&& albumSongs.get(0).getYear() != null){
            album_Name.setText(albumSongs.get(0).getAlbum());
            album_artist.setText(albumSongs.get(0).getArtist());
            albumYear.setText(albumSongs.get(0).getYear());
        }else{
            album_Name.setText(R.string.unbekanntes_album);
            albumYear.setText(R.string.unbekanntes_jahr);
        }

        if (!albumSongs.isEmpty()) {
            try {
                long albumId = Long.parseLong(albumSongs.get(0).getAlbumId());
                Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
                Uri albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumId);
                Glide.with(this)
                        .load(albumArtUri)
                        .placeholder(R.mipmap.ic_play)
                        .error(R.mipmap.ic_play)
                        .into(albumPhoto);
                albumYear.setText(albumSongs.get(0).getYear());
            } catch (NumberFormatException e) {
                Glide.with(this).load(R.mipmap.ic_play).into(albumPhoto);
            }
        }

        albumDetailsAdapter = new AlbumDetailsAdapter(this, albumSongs);
        recyclerView.setAdapter(albumDetailsAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (albumName == null) return; // Sicherheitscheck    // --- NEUE, ROBUSTE LOGIK ---
        // 1. Hole die Songs für DIESES Album, direkt vom System und nach Track-Nummer sortiert.
        albumSongs = getAlbumSongs(this, albumName);

        // 2. Sage dem Adapter, er soll sich mit der neuen, perfekt sortierten Liste aktualisieren.
        if (albumDetailsAdapter != null) {
            albumDetailsAdapter.updateList(albumSongs);
        }
    }

    private byte[] getAlbumArt(String uri){
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri);
        byte[] art = retriever.getEmbeddedPicture();
        try {
            retriever.release();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return art;
    }
    public ArrayList<MusicFiles> getAlbumSongs(Context context, String albumName) {
        ArrayList<MusicFiles> albumSongList = new ArrayList<>();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        // Wir wollen nur Songs, deren Album-Name dem übergebenen Namen entspricht.
        String selection = MediaStore.Audio.Media.ALBUM + "=?";
        String[] selectionArgs = new String[]{albumName};

        // Sortierung nach der Track-Nummer
        // Die Track-Nummer ist in MediaStore.Audio.Media.TRACK gespeichert
        String sortOrder = MediaStore.Audio.Media.TRACK + " ASC";

        String[] projection = {
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.YEAR,
                //MediaStore.Audio.Media.GENRE existiert wohl nicht mehr
        };

        Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String album = cursor.getString(0);
                String title = cursor.getString(1);
                String duration = cursor.getString(2);
                String path = cursor.getString(3);
                String artist = cursor.getString(4);
                String id = cursor.getString(5);
                String albumId = cursor.getString(6);
                String albumYear = cursor.getString(7);
                //String albumgenre = cursor.getString(8);
                long dateAdded = System.currentTimeMillis();
                long size = new java.io.File(path).length();



                MusicFiles musicFile = new MusicFiles(path, title, artist, album, duration, id,albumId,albumYear,dateAdded,size);
                albumSongList.add(musicFile);
            }
            cursor.close();
        }
        return albumSongList;
    }

}