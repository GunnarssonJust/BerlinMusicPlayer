package com.berlinmusicplayer;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class PlaylistFragment extends Fragment {

    private RecyclerView recyclerView;
    private PlaylistAdapter playlistAdapter;
    private PlaylistManager playlistManager;

    // ← M3U Datei-Picker
    private final ActivityResultLauncher<String[]> m3uPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    importM3uFile(uri);
                }
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);

        recyclerView = view.findViewById(R.id.playlist_recyclerView);
        FloatingActionButton fabNewPlaylist = view.findViewById(R.id.fab_new_playlist);
        FloatingActionButton fabImportPlaylist = view.findViewById(R.id.fab_import_playlist);

        playlistManager = PlaylistManager.getInstance(requireContext());

        playlistAdapter = new PlaylistAdapter();
        recyclerView.setAdapter(playlistAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        fabNewPlaylist.setOnClickListener(v -> showCreatePlaylistDialog());

        // ← Import-FAB öffnet Datei-Picker
        fabImportPlaylist.setOnClickListener(v ->
                m3uPicker.launch(new String[]{"audio/x-mpegurl", "application/octet-stream", "*/*"})
        );

        return view;
    }

    // ─── M3U Datei einlesen ───────────────────────────────────────
    private void importM3uFile(Uri uri) {
        String playlistName = getFileNameFromUri(uri);
        ArrayList<String> paths = new ArrayList<>();

        try (InputStream is = requireContext().getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // M3U Kommentare und leere Zeilen überspringen
                if (line.isEmpty() || line.startsWith("#")) continue;
                paths.add(line);
            }

        } catch (Exception e) {
            Log.e("PlaylistFragment", "Fehler beim Lesen der M3U Datei", e);
            Toast.makeText(getContext(), "Fehler beim Lesen der Datei!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (paths.isEmpty()) {
            Toast.makeText(getContext(), "Keine Songs in der Datei gefunden!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Songs mit MusicFiles abgleichen
        ArrayList<MusicFiles> allSongs = playlistManager.getMusicFiles();
        if (allSongs == null) {
            Toast.makeText(getContext(), "Musikbibliothek nicht geladen!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Neue Playlist erstellen
        playlistManager.createPlaylist(playlistName);
        int newIndex = playlistManager.getPlaylists().size() - 1;
        int found = 0;

        for (String path : paths) {
            for (MusicFiles song : allSongs) {
                // Pfad oder Dateiname vergleichen
                if (song.getPath().equals(path) ||
                        song.getPath().endsWith(path) ||
                        path.endsWith(getFileName(song.getPath()))) {
                    playlistManager.addSongToPlaylist(newIndex, song.getId());
                    found++;
                    break;
                }
            }
        }

        playlistAdapter.notifyDataSetChanged();
        Toast.makeText(getContext(),
                "\"" + playlistName + "\" importiert — " + found + "/" + paths.size() + " Songs gefunden!",
                Toast.LENGTH_LONG).show();
    }

    // ─── Hilfsmethoden ────────────────────────────────────────────
    private String getFileNameFromUri(Uri uri) {
        String path = uri.getLastPathSegment();
        if (path != null) {
            // .m3u Endung entfernen
            if (path.contains("/")) path = path.substring(path.lastIndexOf("/") + 1);
            if (path.toLowerCase().endsWith(".m3u")) path = path.substring(0, path.length() - 4);
        }
        return path != null ? path : "Importierte Playlist";
    }

    private String getFileName(String path) {
        if (path == null) return "";
        return path.contains("/") ? path.substring(path.lastIndexOf("/") + 1) : path;
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

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), PlaylistDetailActivity.class);
                intent.putExtra("playlistIndex", position);
                intent.putExtra("playlistName", playlist.name);
                startActivity(intent);
            });

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
