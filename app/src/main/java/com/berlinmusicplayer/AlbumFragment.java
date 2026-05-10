package com.berlinmusicplayer;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

public class AlbumFragment extends Fragment {
    RecyclerView recyclerView;
    AlbumAdapter albumAdapter;
    private ArrayList<MusicFiles> albumList = new ArrayList<>();

    public AlbumFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_album, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        if (getActivity() instanceof MainActivity) {
            albumList = (((MainActivity) getActivity()).getAllAlbum());
        }
        recyclerView.setHasFixedSize(true);

        albumAdapter = new AlbumAdapter(getContext(),albumList);
        recyclerView.setAdapter(albumAdapter);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(),2));
        setAlbumList(albumList);
        return view;
    }

    public void updateAdapterList(ArrayList<MusicFiles> filteredList) {
        if (albumAdapter != null) {
            albumAdapter.updateList(filteredList);
        }
    }

    public void setAlbumList(ArrayList<MusicFiles> albumList) {
        if(albumList == null) return;
        this.albumList = albumList;
        if(albumAdapter != null){
            albumAdapter.updateList(albumList);
        }
    }
}