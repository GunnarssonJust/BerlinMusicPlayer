package com.berlinmusicplayer;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class ArtistDetailAdapter extends RecyclerView.Adapter {
    private final Context mContext;
    ArrayList<MusicFiles> artistDetailFiles;

    public ArtistDetailAdapter(Context mContext, ArrayList<MusicFiles> artistFiles) {
        this.mContext = mContext;
        artistDetailFiles = artistFiles;
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

        holder.itemView.setOnClickListener(v -> {
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
}

