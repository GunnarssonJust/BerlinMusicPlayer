package com.berlinmusicplayer;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Collections;

public class ArtistAdapter extends RecyclerView.Adapter<ArtistAdapter.MyHolder> {
    private Context mContext;
    ArrayList<MusicFiles> artistFiles;

    public ArtistAdapter(Context mContext, ArrayList<MusicFiles> artistFiles) {
        this.mContext = mContext;
        this.artistFiles = artistFiles;
    }

    @NonNull
    @Override
    public ArtistAdapter.MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.album_item, parent, false);
        return new ArtistAdapter.MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArtistAdapter.MyHolder holder, @SuppressLint("RecyclerView") final int position) {
        // 1. Setze den Künstlernamen
        holder.artist_name.setText(artistFiles.get(position).getArtist());

        // 2. Lade das Album-Cover performant (genau wie im AlbumAdapter)
        // Cover des repräsentativen Songs für diesen Künstler.
        Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
        String albumId = artistFiles.get(position).getAlbumId();
        Uri albumArtUri = ContentUris.withAppendedId(sArtworkUri, Long.parseLong(albumId));

        Glide.with(mContext)
                .load(albumArtUri)
                .placeholder(R.drawable.ic_play_btn) // Guter Platzhalter
                .error(R.drawable.ic_adler)       // Gutes Fehlerbild
                .into(holder.artist_image);

        // 3. Setze den OnClickListener mit der korrekten Logik
        holder.itemView.setOnClickListener(v -> {
            // Starte die ArtistDetails-Activity und übergib den KÜNSTLERNAMEN
            Intent intent = new Intent(mContext, ArtistActivity.class);
            intent.putExtra("artistName", artistFiles.get(position).getArtist());
            mContext.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return artistFiles.size();
    }

    public class MyHolder extends RecyclerView.ViewHolder {
        ImageView artist_image;
        TextView artist_name;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            artist_image = itemView.findViewById(R.id.albumitem_image);
            artist_name = itemView.findViewById(R.id.album_subtext);
        }
    }

    // Saubere Update-Methode mit alphabetischer Sortierung der Künstler
    void updateList(ArrayList<MusicFiles> artistFilesArrayList) {
        artistFiles = new ArrayList<>(artistFilesArrayList);
        // Sortiere die Künstlerliste immer alphabetisch, ignoriere Groß/Kleinschreibung
        Collections.sort(artistFiles, (a, b) -> a.getArtist().compareToIgnoreCase(b.getArtist()));
        notifyDataSetChanged();
    }
}
