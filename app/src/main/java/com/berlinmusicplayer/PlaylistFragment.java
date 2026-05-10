package com.berlinmusicplayer;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class PlaylistFragment extends Fragment {

    private RecyclerView recyclerView;
    private PlaylistAdapter playlistAdapter;
    private PlaylistManager playlistManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);

        recyclerView = view.findViewById(R.id.playlist_recyclerView);
        FloatingActionButton fabNewPlaylist = view.findViewById(R.id.fab_new_playlist);

        playlistManager = PlaylistManager.getInstance(requireContext());

        playlistAdapter = new PlaylistAdapter();
        recyclerView.setAdapter(playlistAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Neue Playlist erstellen
        fabNewPlaylist.setOnClickListener(v -> showCreatePlaylistDialog());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        playlistAdapter.notifyDataSetChanged();
    }

    private void showCreatePlaylistDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("Playlist-Name");

        new AlertDialog.Builder(requireContext())
                .setTitle("Neue Playlist")
                .setView(input)
                .setPositiveButton("Erstellen", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        playlistManager.createPlaylist(name);
                        playlistAdapter.notifyDataSetChanged();
                    }
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    private void showRenameDialog(int index) {
        EditText input = new EditText(requireContext());
        input.setText(playlistManager.getPlaylists().get(index).name);

        new AlertDialog.Builder(requireContext())
                .setTitle("Umbenennen")
                .setView(input)
                .setPositiveButton("Speichern", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        playlistManager.renamePlaylist(index, name);
                        playlistAdapter.notifyDataSetChanged();
                    }
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    private void showDeleteDialog(int index) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Playlist löschen")
                .setMessage("\"" + playlistManager.getPlaylists().get(index).name + "\" wirklich löschen?")
                .setPositiveButton("Löschen", (dialog, which) -> {
                    playlistManager.deletePlaylist(index);
                    playlistAdapter.notifyDataSetChanged();
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    // ─── Adapter ──────────────────────────────────────────────────
    private class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistHolder> {

        @NonNull
        @Override
        public PlaylistHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.item_playlist, parent, false);
            return new PlaylistHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PlaylistHolder holder, int position) {
            PlaylistManager.Playlist playlist = playlistManager.getPlaylists().get(position);
            holder.name.setText(playlist.name);
            holder.count.setText(playlist.songIds.size() + " Songs");

            // Playlist öffnen
            holder.itemView.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(getContext(), PlaylistDetailActivity.class);
                intent.putExtra("playlistIndex", position);
                intent.putExtra("playlistName", playlist.name);
                startActivity(intent);
            });

            // Menü: Umbenennen / Löschen
            holder.menuMore.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(getContext(), v);
                popupMenu.getMenu().add(0, 0, 0, "Umbenennen");
                popupMenu.getMenu().add(0, 1, 1, "Löschen");
                popupMenu.show();
                popupMenu.setOnMenuItemClickListener(item -> {
                    int adapterPosition = holder.getAdapterPosition();
                    if (item.getItemId() == 0) {
                        showRenameDialog(adapterPosition);
                    } else if (item.getItemId() == 1) {
                        showDeleteDialog(adapterPosition);
                    }
                    return true;
                });
            });
        }

        @Override
        public int getItemCount() {
            return playlistManager.getPlaylists().size();
        }

        class PlaylistHolder extends RecyclerView.ViewHolder {
            TextView name, count;
            ImageView menuMore;

            PlaylistHolder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.playlist_name);
                count = itemView.findViewById(R.id.playlist_count);
                menuMore = itemView.findViewById(R.id.playlist_menu);
            }
        }
    }
}
