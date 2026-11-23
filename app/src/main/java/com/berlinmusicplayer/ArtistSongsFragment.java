package com.berlinmusicplayer;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
@SuppressLint("UnspecifiedRegisterReceiverFlag")
public class ArtistSongsFragment extends Fragment {
    private static final String ARG_ARTIST = "artistName";
    private String artistName;
    private static final String TAG = "BINDUNGSHAFTUNG";
    private ArrayList<MusicFiles> artistSongs;
    private BroadcastReceiver songChangeReceiver;
    private SongListAdapter adapter;
    private RecyclerView recyclerView;

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
        recyclerView = view.findViewById(R.id.tab_recyclerView);
        if (getArguments() != null) {
            artistName = getArguments().getString(ARG_ARTIST);
            artistSongs = getArguments().getParcelableArrayList("songs");
        }
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        if (artistSongs != null) {
            adapter = new SongListAdapter(getContext(), artistSongs, 0, false, false);
            recyclerView.setAdapter(adapter);
        }
        view.findViewById(R.id.shuffle_all_songs).setOnClickListener(v -> shuffleAll(artistSongs));
        return view;
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onStart() {
        super.onStart();
        songChangeReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "SONG_CHANGED empfangen");
                if (adapter != null) {
                    recyclerView.post(()->adapter.notifyDataSetChanged());
                    Log.d("ESEL", "Adapter wird aktualisiert");
                    Log.d("BINDUNGSHAFTUNG", "notifyDataSetChanged()");
                } else {
                    Log.d("ESEL", "Adapter ist null");
                }
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requireActivity().registerReceiver(songChangeReceiver, new IntentFilter("SONG_CHANGED"), Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireActivity().registerReceiver(songChangeReceiver, new IntentFilter("SONG_CHANGED"));
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (songChangeReceiver != null) {
            requireActivity().unregisterReceiver(songChangeReceiver);
            songChangeReceiver = null;
        }
    }

    public void refreshAdapter() {
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void shuffleAll(ArrayList<MusicFiles> list) {
        if (list == null || list.isEmpty()) return;
        MusicService s = (requireActivity() instanceof BaseActivity)
                ? ((BaseActivity) requireActivity()).getMusicService() : null;
        if (s == null) {
            Toast.makeText(requireContext(), "Player nicht verbunden", Toast.LENGTH_SHORT).show();
            return;
        }
        s.shufflePlaylist(new ArrayList<>(list));
        startActivity(new Intent(requireContext(), PlayerActivity.class));
    }
}



