package com.berlinmusicplayer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ArtistSongsFragment extends Fragment {

    private static final String ARG_ARTIST = "artistName";
    private String artistName;
    private ArrayList<MusicFiles> artistSongs;

    public static ArtistSongsFragment newInstance(String artistName, ArrayList<MusicFiles> songs) {
        ArtistSongsFragment fragment = new ArtistSongsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ARTIST, artistName);
        args.putParcelableArrayList("songs", songs);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_artist_tab, container, false);

        if (getArguments() != null) {
            artistName = getArguments().getString(ARG_ARTIST);
            artistSongs = getArguments().getParcelableArrayList("songs");
        }

        RecyclerView recyclerView = view.findViewById(R.id.tab_recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        if (artistSongs != null) {
            ArtistDetailAdapter adapter = new ArtistDetailAdapter(getContext(), artistSongs);
            recyclerView.setAdapter(adapter);
        }

        return view;
    }
}
