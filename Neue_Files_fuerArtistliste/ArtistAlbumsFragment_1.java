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
                    // Jedes Album nur einmal hinzufügen
                    if (!addedAlbums.contains(album)) {
                        addedAlbums.add(album);
                        String title = cursor.getString(1);
                        String duration = cursor.getString(2);
                        String path = cursor.getString(3);
                        String artist = cursor.getString(4);
                        String id = cursor.getString(5);
                        String albumId = cursor.getString(6);
                        albumList.add(new MusicFiles(path, title, artist, album, duration, id, albumId));
                    }
                }
            }
        }
        return albumList;
    }
}
