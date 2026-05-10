package com.berlinmusicplayer;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

public class ArtistFragment extends Fragment {

    private RecyclerView recyclerView;
    private ArtistAdapter artistAdapter;
    private ArrayList<MusicFiles> artist = new ArrayList<>();
    // Required empty public constructor, notwendiger Constructor
    public ArtistFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d("ArtistFragment", "Esel ArtistFragment: onCreateView");
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_artist, container, false);
        recyclerView = view.findViewById(R.id.recyclerView_artist);
        if (getActivity() instanceof MainActivity) {
            artist = (((MainActivity) getActivity()).getAllArtist());
        }

        recyclerView.setHasFixedSize(true);

        artistAdapter = new ArtistAdapter(getContext(),artist);
        recyclerView.setAdapter(artistAdapter);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(),2));
        setArtistList(artist);
        return view;

    }

    public void updateAdapterList(ArrayList<MusicFiles> filteredList) {
        Log.d("ArtistFragment", "Esel ArtistFragment: updateAdapterList");
        if (artistAdapter != null) {
            artistAdapter.updateList(filteredList);
        }
    }

    // 👉 zentrale Methode
    public void setArtistList(ArrayList<MusicFiles> artist) {
        Log.d("ArtistFragment", "Esel ArtistFragment: setArtistList");
        if (artist == null) return;

        this.artist = artist;

        if (artistAdapter != null) {
            artistAdapter.updateList(artist);
        }
    }
}