package com.berlinmusicplayer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class RecentFragment extends Fragment {

    private RecyclerView recyclerView;
    private ArtistDetailAdapter adapter;
    private TextView emptyText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recent, container, false);

        recyclerView = view.findViewById(R.id.recent_recyclerView);
        emptyText = view.findViewById(R.id.recent_empty_text);

        ArrayList<MusicFiles> recentSongs = PlaylistManager.getInstance(requireContext()).getRecentSongs();

        if (recentSongs.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter = new ArtistDetailAdapter(getContext(), recentSongs);
            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        ArrayList<MusicFiles> recentSongs = PlaylistManager.getInstance(requireContext()).getRecentSongs();
        if (adapter != null) {
            adapter.updateList(recentSongs);
        } else if (!recentSongs.isEmpty()) {
            emptyText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter = new ArtistDetailAdapter(getContext(), recentSongs);
            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }
    }
}
