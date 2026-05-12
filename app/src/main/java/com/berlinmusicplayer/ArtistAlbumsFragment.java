package com.berlinmusicplayer;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ArtistAlbumsFragment extends Fragment {

    private static final String ARG_ARTIST = "artistName";
    private String artistName;

    public static ArtistAlbumsFragment newInstance(String artistName) {
        ArtistAlbumsFragment fragment = new ArtistAlbumsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ARTIST, artistName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_artist_tab, container, false);

        if (getArguments() != null) {
            artistName = getArguments().getString(ARG_ARTIST);
        }

        RecyclerView recyclerView = view.findViewById(R.id.tab_recyclerView);
        // Alben im Grid (2 Spalten) wie im AlbumFragment
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        ArrayList<MusicFiles> albums = getAlbumsForArtist(getContext(), artistName);
        AlbumAdapter adapter = new AlbumAdapter(getContext(), albums);
        recyclerView.setAdapter(adapter);

        return view;
    }

    private ArrayList<MusicFiles> getAlbumsForArtist(Context context, String artistName) {
        ArrayList<MusicFiles> albumList = new ArrayList<>();
        ArrayList<String> addedAlbums = new ArrayList<>(); // Doppelte Alben vermeiden

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.ARTIST + "=?";
        String[] selectionArgs = new String[]{artistName};
        String sortOrder = MediaStore.Audio.Media.ALBUM + " COLLATE NOCASE ASC";

        String[] projection = {
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.YEAR,
                //MediaStore.Audio.Media.GENRE,
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
                //int albumGenreIdx = cursor.getColumnIndex(MediaStore.Audio.Media.GENRE);
                int dateAddedIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED);
                int sizeIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);

                while (cursor.moveToNext()) {
                    String album = cursor.getString(albumColumn);
                    // Jedes Album nur einmal hinzufügen
                    if (!addedAlbums.contains(album)) {
                        addedAlbums.add(album);
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

                        albumList.add(new MusicFiles(path, title, artist, album, duration, id, albumId,albumYear,dateAdded,size));
                    }
                }
            }
        }
        return albumList;
    }
}
