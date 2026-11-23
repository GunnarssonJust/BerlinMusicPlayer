package com.berlinmusicplayer;

import static com.berlinmusicplayer.MusicFiles.formatDuration;

import androidx.activity.EdgeToEdge;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.util.ArrayList;

public class AlbumDetails extends BaseActivity {
    RecyclerView recyclerView;
    ImageView albumPhoto;
    String albumName, albumId;
    TextView album_Name, album_artist, albumYear, album_duration;
    ArrayList<MusicFiles> albumSongs = new ArrayList<>();
    AlbumDetailsAdapter albumDetailsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_details);

        getWindow().setStatusBarColor(getColor(R.color.darkgreen));
        getWindow().setNavigationBarColor(getColor(R.color.darkestgreen));
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);

        recyclerView = findViewById(R.id.album_recyclerView);
        albumPhoto = findViewById(R.id.albumPhoto);
        albumName = getIntent().getStringExtra("albumName");
        albumId = getIntent().getStringExtra("albumId");
        album_Name = findViewById(R.id.album_name);
        album_artist = findViewById(R.id.artist);
        albumYear = findViewById(R.id.album_details_year);
        album_duration = findViewById(R.id.album_details_duration);

        albumSongs = getAlbumSongs(this, albumName);

        if (!albumSongs.isEmpty()) {
            updateAlbumHeader();
        } else {
            album_Name.setText(R.string.unbekanntes_album);
            album_artist.setText(R.string.unbekanner_artist);
            albumYear.setText(R.string.unbekanntes_jahr);
            album_duration.setText(R.string.duration);
        }

        if (!albumSongs.isEmpty()) {
            try {
                long albumIds = Long.parseLong(albumSongs.get(0).getAlbumId());
                Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
                Uri albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumIds);
                Glide.with(this)
                        .load(albumArtUri)
                        .placeholder(R.mipmap.ic_play)
                        .error(R.mipmap.ic_play)
                        .into(albumPhoto);
            } catch (NumberFormatException e) {
                Glide.with(this).load(R.mipmap.ic_play).into(albumPhoto);
            }
        }

        albumDetailsAdapter = new AlbumDetailsAdapter(this, albumSongs);
        recyclerView.setAdapter(albumDetailsAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
    }

    @Override
    protected void onMusicServiceConnected() {
        // Diese Methode wird aufgerufen, sobald die Verbindung zum MusicService steht.
        if (albumDetailsAdapter != null) {
            albumDetailsAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (albumName == null) return;
        albumSongs = getAlbumSongs(this, albumName);

        if (albumDetailsAdapter != null) {
            albumDetailsAdapter.updateList(albumSongs);
            albumDetailsAdapter.notifyDataSetChanged();
        }
        updateAlbumHeader();
    }

    public ArrayList<MusicFiles> getAlbumSongs(Context context, String albumName) {
        ArrayList<MusicFiles> albumSongList = new ArrayList<>();
        if (albumName == null) return albumSongList;

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection;
        String[] selectionArgs;

        if (albumId != null) {
            selection = MediaStore.Audio.Media.ALBUM + "=? AND " +
                    MediaStore.Audio.Media.ALBUM_ID + "=?";
            selectionArgs = new String[]{albumName, albumId};
        } else {
            selection = MediaStore.Audio.Media.ALBUM + "=?";
            selectionArgs = new String[]{albumName};
        }

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
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.SIZE
        };

        try (Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder)) {
            if (cursor != null) {
                int albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                int titleIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int durationIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                int pathIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                int artistIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int idIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int albumIdIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
                int albumYearIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR);
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
                    long dateAdded = cursor.getLong(dateAddedIdx);
                    long size = cursor.getLong(sizeIdx);

                    MusicFiles musicFile = new MusicFiles(path, title, artist, album, duration, id, albumId, albumYear, dateAdded, size);
                    albumSongList.add(musicFile);
                }
            }
        }
        return albumSongList;
    }

    private void updateAlbumHeader() {
        if (!albumSongs.isEmpty()) {
            album_Name.setText(albumSongs.get(0).getAlbum());
            album_artist.setText(albumSongs.get(0).getArtist());
            
            try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
                retriever.setDataSource(albumSongs.get(0).getPath());
                String year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR);
                
                Log.d("YEAR_DEBUG", "Album=" + albumSongs.get(0).getAlbum() +
                        "Year= " + year + "Duration=" + albumSongs.get(0).getDuration());
                albumYear.setText(
                        year != null && !year.isEmpty()
                                ? year
                                : getString(R.string.unbekanntes_jahr)
                );
            } catch (IOException | IllegalArgumentException e) {
                albumYear.setText(R.string.unbekanntes_jahr);
            }

            long totalMillis = 0;
            for (MusicFiles song : albumSongs) {
                try {
                    totalMillis += Long.parseLong(song.getDuration());
                } catch (NumberFormatException ignored) {
                }
            }
            album_duration.setText(formatDuration(String.valueOf(totalMillis)));
        }
    }

    private BroadcastReceiver songChangeReceiver;

    @Override
    protected void onStart() {
        super.onStart();
        songChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (albumDetailsAdapter != null) albumDetailsAdapter.notifyDataSetChanged();
            }
        };
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(songChangeReceiver,
                    new IntentFilter("SONG_CHANGED"), Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(songChangeReceiver, new IntentFilter("SONG_CHANGED"));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (songChangeReceiver != null) {
            unregisterReceiver(songChangeReceiver);
        }
    }
}
