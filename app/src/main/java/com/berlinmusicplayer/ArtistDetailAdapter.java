package com.berlinmusicplayer;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class ArtistDetailAdapter extends RecyclerView.Adapter {
    private final Context mContext;
    private MusicService musicService;
    ArrayList<MusicFiles> artistDetailFiles;
    int playlistIndex;


    public ArtistDetailAdapter(Context mContext, ArrayList<MusicFiles> artistFiles, int playlistIndex) {
        this.mContext = mContext;
        artistDetailFiles = artistFiles;
        this.playlistIndex = playlistIndex;
    }

    @NonNull

    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.music_items, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MyHolder myHolder = (MyHolder) holder;
        MusicFiles song = artistDetailFiles.get(position);
        myHolder.song_name.setText(song.getTitle());
        myHolder.artist_name.setText(song.getAlbum()); // Zeige Album-Name statt Artist (ist ja klar wer der Artist ist)
        // ← Prüfe ob das der aktuelle Song ist
        if (isCurrentSong(song)) {
            myHolder.song_name.setTextColor(mContext.getColor(android.R.color.holo_blue_bright));
            myHolder.artist_name.setTextColor(mContext.getColor(android.R.color.holo_blue_bright));
        } else {
            myHolder.song_name.setTextColor(mContext.getColor(android.R.color.white));
            myHolder.artist_name.setTextColor(mContext.getColor(android.R.color.darker_gray));
        }

        // Album-Cover laden
        try {
            long albumId = Long.parseLong(song.getAlbumId());
            Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
            Uri albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumId);
            Glide.with(mContext)
                    .load(albumArtUri)
                    .placeholder(R.mipmap.ic_play)
                    .error(R.mipmap.ic_play)
                    .into(myHolder.album_image);
        } catch (NumberFormatException e) {
            Glide.with(mContext).load(R.mipmap.ic_play).into(myHolder.album_image);
        }
        ImageView menuMore = myHolder.itemView.findViewById(R.id.menuMore);
        menuMore.setOnClickListener(v -> {

            PopupMenu popupMenu = new PopupMenu(mContext, v);

            popupMenu.getMenu().add(0, 0, 0, "Hinzufügen");
            popupMenu.getMenu().add(0, 0, 0, "Aus Liste entfernen");

            popupMenu.setOnMenuItemClickListener(item -> {

                if(item.getTitle().equals("Aus Liste entfernen")) {

                    new AlertDialog.Builder(mContext)
                            .setTitle("Song aus der Liste entfernen?")
                            .setMessage(song.getTitle() + " - " + song.getArtist())
                            .setPositiveButton("Ja", (dialog, which) -> {

                                PlaylistManager.getInstance(mContext)
                                        .removeSongFromPlaylist(playlistIndex, song.getId());

                                artistDetailFiles.remove(position);

                                notifyItemRemoved(position);

                            })
                            .setNegativeButton("Nein", null)
                            .show();

                    return true;

                } else if(item.getTitle().equals("Hinzufügen")) {

                    PlaylistManager manager =
                            PlaylistManager.getInstance(mContext);

                    ArrayList<PlaylistManager.Playlist> playlists =
                            manager.getPlaylists();

                    if(playlists.isEmpty()) {

                        Toast.makeText(
                                mContext,
                                "Keine Playlists vorhanden!",
                                Toast.LENGTH_SHORT
                        ).show();

                        return true;
                    }

                    String[] names = new String[playlists.size()];

                    for (int i = 0; i < playlists.size(); i++) {
                        names[i] = playlists.get(i).name;
                    }

                    String songId = song.getId();

                    new AlertDialog.Builder(mContext)
                            .setTitle("Zur Playlist hinzufügen")
                            .setItems(names, (dialog, which) -> {

                                manager.addSongToPlaylist(which, songId);

                                Toast.makeText(
                                        mContext,
                                        "Song zu \"" +
                                                playlists.get(which).name +
                                                "\" hinzugefügt!",
                                        Toast.LENGTH_SHORT
                                ).show();

                            })
                            .setNegativeButton("Abbrechen", null)
                            .show();
                    return true;
                }
                return false;
            });
            popupMenu.show();
        });

        myHolder.itemView.setOnClickListener(v -> {
            // Service starten mit der Artist-Playlist
            Intent serviceIntent = new Intent(mContext, MusicService.class);
            serviceIntent.putExtra("actionName", "PLAY_ARTIST_SONG");
            serviceIntent.putParcelableArrayListExtra("artistSongs", artistDetailFiles);
            serviceIntent.putExtra("position", position);
            mContext.startService(serviceIntent);

            // PlayerActivity öffnen
            Intent playerIntent = new Intent(mContext, PlayerActivity.class);
            playerIntent.putParcelableArrayListExtra("playlist", artistDetailFiles);
            playerIntent.putExtra("position", position);
            playerIntent.putExtra("sender", "artistDetails");
            playerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(playerIntent);
        });
    }

    @Override
    public int getItemCount() {
        return artistDetailFiles != null ? artistDetailFiles.size() : 0;
    }

    public static class MyHolder extends RecyclerView.ViewHolder {
        ImageView album_image;
        TextView song_name, artist_name;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            album_image = itemView.findViewById(R.id.music_img);
            song_name = itemView.findViewById(R.id.music_file_name);
            artist_name = itemView.findViewById(R.id.artist_file_name);
        }
    }

    void updateList(ArrayList<MusicFiles> newFiles) {
        artistDetailFiles = new ArrayList<>(newFiles);
        notifyDataSetChanged();
    }
    private boolean isCurrentSong(MusicFiles song) {
        SharedPreferences prefs = mContext.getSharedPreferences("LAST_PLAYED", Context.MODE_PRIVATE);
        String currentSongId = prefs.getString("current_song_id", "");
        return currentSongId.equals(song.getId());
    }
}

