package com.berlinmusicplayer;

import static com.berlinmusicplayer.AlbumDetailsAdapter.albumFiles;

import android.annotation.SuppressLint;
import android.content.ContentUris;import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.io.File;
import java.util.ArrayList;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MyViewHolder> implements
        FastScrollRecyclerView.SectionedAdapter {

    private Context mContext;
    private ArrayList<MusicFiles> mFiles;
    private boolean isInSearchMode = false;
    MusicService musicService;

    MusicAdapter(Context mContext, ArrayList<MusicFiles> mFiles) {
        this.mContext = mContext;
        this.mFiles = mFiles;
    }

    // --- METHODE 1: WIE SIEHT EINE LEERE ZEILE AUS? ---
    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // "Blase" das Layout music_items.xml auf und erstelle einen ViewHolder (Karton) dafür.
        View view = LayoutInflater.from(mContext).inflate(R.layout.music_items, parent, false);
        return new MyViewHolder(view);
    }

    // --- METHODE 2: WIE BEFÜLLE ICH DIE ZEILE MIT DATEN? ---
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, @SuppressLint("RecyclerView") final int position) {

        holder.file_name.setText(mFiles.get(position).getTitle());
        holder.artist_name.setText(mFiles.get(position).getArtist());
        MusicFiles currentSong = mFiles.get(position);

        if (isCurrentSong(currentSong)) {
            holder.file_name.setTextColor(mContext.getColor(android.R.color.holo_blue_bright));
            holder.artist_name.setTextColor(mContext.getColor(android.R.color.holo_blue_bright));
        } else {
            holder.file_name.setTextColor(mContext.getColor(android.R.color.white));
            holder.artist_name.setTextColor(mContext.getColor(android.R.color.darker_gray));
        }



        holder.trackNumber.setVisibility(View.GONE);
        holder.album_art.setVisibility(View.VISIBLE);

        Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
        Uri albumcover_uri = ContentUris.withAppendedId(sArtworkUri, Long.parseLong(mFiles.get(position).getAlbumId()));

        Glide.with(mContext)
                .load(albumcover_uri)
                .placeholder(R.drawable.ic_adler) // Fallback-Icon
                .error(R.drawable.ic_adler)       // Icon bei Ladefehler
                .into(holder.album_art);


        holder.menuMore.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(mContext, v);
            popupMenu.getMenuInflater().inflate(R.menu.popup, popupMenu.getMenu());
            popupMenu.show();
            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.delete) {
                    deleteFile(position, v);
                }else if (item.getItemId() == R.id.add_to_playlist) {
                    showAddToPlaylistDialog(position, v);
                }
                return true;
            });
        });


        holder.itemView.setOnClickListener(v -> {
            // --- HIER IST DIE FINALE, SAUBERE LÖSUNG ---
            Log.d("MusicAdapter", "Klick in Hauptliste/Suche auf Position " + position);

            // 1. Erstelle den EINEN, klaren und robusten "Reset"-Befehl für den Service.
            Intent serviceIntent = new Intent(mContext, MusicService.class);
            if(isInSearchMode){
                serviceIntent.putExtra("actionName", "PLAY_ALBUM_SONG");
                serviceIntent.putParcelableArrayListExtra("playlist", mFiles);
            }else{
                serviceIntent.putExtra("actionName", "PLAY_MASTER_SONG");
            }
            serviceIntent.putExtra("position", position);
            // 2. Sende den Befehl an den Service. Das System kümmert sich um das Timing.
            mContext.startService(serviceIntent);

            // 3. Öffne die PlayerActivity, damit der Benutzer die UI sieht.
            //    Dieser Intent ist jetzt "dumm" und enthält keine Wiedergabe-Infos mehr.
            Intent playerIntent = new Intent(mContext, PlayerActivity.class);
            playerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(playerIntent);
            // ---------------------------------------------
        });
    }

    private void showAddToPlaylistDialog(int position, View v) {
        PlaylistManager manager = PlaylistManager.getInstance(mContext);
        ArrayList<PlaylistManager.Playlist> playlists = manager.getPlaylists();

        if (playlists.isEmpty()) {
            com.google.android.material.snackbar.Snackbar.make(v,
                    "Keine Playlists vorhanden. Erst eine Playlist erstellen!",
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show();
            return;
        }

        String[] names = new String[playlists.size()];
        for (int i = 0; i < playlists.size(); i++) {
            names[i] = playlists.get(i).name;
        }

        new android.app.AlertDialog.Builder(mContext)
                .setTitle("Zur Playlist hinzufügen")
                .setItems(names, (dialog, which) -> {
                    manager.addSongToPlaylist(which, mFiles.get(position).getId());
                    com.google.android.material.snackbar.Snackbar.make(v,
                            "Song zu \"" + playlists.get(which).name + "\" hinzugefügt!",
                            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    // --- METHODE 3: WIE VIELE ZEILEN GIBT ES? ---
    @Override
    public int getItemCount() {
        // Sicherheitscheck: Wenn die Liste null ist, gibt es 0 Items.
        if (mFiles == null) {
            return 0;
        }
        return mFiles.size();
    }

    // Methode, um die Liste von außen zu aktualisieren (für Suche & Sortierung)
    public void updateList(ArrayList<MusicFiles> newList,boolean isSearchResult) {
        final MusicDiffCallback diffCallback = new MusicDiffCallback(this.mFiles, newList);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        this.isInSearchMode = isSearchResult;
        this.mFiles = new ArrayList<>(newList);
        diffResult.dispatchUpdatesTo(this);
    }

    // Methode zum Löschen einer Datei
    private void deleteFile(int position, View v) {
        // Sicherheitscheck
        if (position >= mFiles.size()) return;

        Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                Long.parseLong(mFiles.get(position).getId()));

        File file = new File(mFiles.get(position).getPath());
        if (file.delete()) {
            // Erst wenn die Datei wirklich gelöscht wurde:
            mContext.getContentResolver().delete(contentUri, null, null);
            mFiles.remove(position); // Entferne das Item aus der Liste
            notifyItemRemoved(position); // Sag dem RecyclerView, dass genau dieses Item weg ist
            notifyItemRangeChanged(position, mFiles.size()); // Aktualisiere die Positionen der restlichen Items
            Snackbar.make(v, "Song gelöscht!", Snackbar.LENGTH_LONG).show();
        } else {
            Snackbar.make(v, "Song konnte nicht gelöscht werden", Snackbar.LENGTH_LONG).show();
        }
    }

    // Der "Karton", der die Views einer einzelnen Zeile enthält
    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView file_name, artist_name, trackNumber;
        ImageView album_art, menuMore;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            file_name = itemView.findViewById(R.id.music_file_name);
            artist_name = itemView.findViewById(R.id.artist_file_name);
            album_art = itemView.findViewById(R.id.music_img);
            menuMore = itemView.findViewById(R.id.menuMore);
            trackNumber = itemView.findViewById(R.id.music_track_number);
        }
    }

    @Override
    public String getSectionName(int position) {
        return mFiles.get(position).getTitle().substring(0, 1).toUpperCase();
    }
    private boolean isCurrentSong(MusicFiles song){
        SharedPreferences prefs = mContext.getSharedPreferences("LAST_PLAYED", Context.MODE_PRIVATE);
        String currentSongId = prefs.getString("current_song_id", "");
        return currentSongId.equals(song.getId());
    }

    public static class MusicDiffCallback extends DiffUtil.Callback {
        private final ArrayList<MusicFiles> oldList;
        private final ArrayList<MusicFiles> newList;

        public MusicDiffCallback(ArrayList<MusicFiles> oldList, ArrayList<MusicFiles> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }
        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            // Prüfe, ob es dasselbe Element ist (basierend auf einer einzigartigen ID).
            return oldList.get(oldItemPosition).getId().equals(newList.get(newItemPosition).getId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            // Prüfe, ob sich der Inhalt (z.B. der Titel) geändert hat.
            return oldList.get(oldItemPosition).getTitle().equals(newList.get(newItemPosition).getTitle());
        }
    }
}
