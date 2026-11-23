package com.berlinmusicplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class RecentFragment extends Fragment {

    private RecyclerView recyclerView;
    private SongListAdapter adapter;
    private TextView emptyText;
    private BroadcastReceiver songChangeReceiver;
    private View fab;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recent, container, false);
        fab = view.findViewById(R.id.shuffle_all_songs);
        recyclerView = view.findViewById(R.id.recent_recyclerView);
        emptyText = view.findViewById(R.id.recent_empty_text);

        showList(PlaylistManager.getInstance(requireContext()).getRecentSongs());
        if(fab != null)fab.setOnClickListener(v -> shuffleAll(
                PlaylistManager.getInstance(requireContext()).getRecentSongs()));
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        showList(PlaylistManager.getInstance(requireContext()).getRecentSongs());
    }

    @Override
    public void onStart() {
        super.onStart();
        songChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (adapter != null) adapter.notifyDataSetChanged();
            }
        };
        ContextCompat.registerReceiver(requireActivity(), songChangeReceiver, new IntentFilter("SONG_CHANGED"), ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (songChangeReceiver != null) {
            requireActivity().unregisterReceiver(songChangeReceiver);
            songChangeReceiver = null;
        }
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

    private void showList(ArrayList<MusicFiles> recentSongs){
        boolean empty = recentSongs.isEmpty();
        emptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        fab.setVisibility(empty ? View.GONE : View.VISIBLE);

        if(empty)return;
        if(adapter == null){
            adapter = new SongListAdapter(getContext(), recentSongs,0,true,false);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.setAdapter(adapter);
        }else{
            adapter.updateList(recentSongs);
        }
    }
}
