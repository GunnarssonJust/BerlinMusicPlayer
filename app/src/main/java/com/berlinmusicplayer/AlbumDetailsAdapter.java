package com.berlinmusicplayer;


import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.util.ArrayList;

public class AlbumDetailsAdapter extends RecyclerView.Adapter<AlbumDetailsAdapter.MyHolder>{
    private Context mContext;
    private TextView trackNumber;
    static ArrayList<MusicFiles> albumFiles;
    View view;
    public AlbumDetailsAdapter(Context mContext, ArrayList<MusicFiles> albumFiles) {
        this.mContext = mContext;
        this.albumFiles = albumFiles;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        view = LayoutInflater.from(mContext).inflate(R.layout.music_items,parent,false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        holder.album_name.setText(albumFiles.get(position).getTitle());
        trackNumber = view.findViewById(R.id.music_track_number);
        holder.album_image.setVisibility(View.GONE);
        holder.artist_name.setVisibility(View.GONE);
        holder.trackNumber.setVisibility(View.VISIBLE);
        String track = getTrackNumber(albumFiles.get(position).getPath());
        holder.trackNumber.setText(track);

        holder.itemView.setOnClickListener(v -> {
            // 1) Service starten mit der Album-Playlist
            Intent serviceIntent = new Intent(mContext, MusicService.class);
            serviceIntent.putExtra("actionName", "PLAY_ALBUM_SONG");
            serviceIntent.putParcelableArrayListExtra("albumSongs", albumFiles);
            serviceIntent.putExtra("position", position);
            mContext.startService(serviceIntent);

            // 2) PlayerActivity öffnen mit derselben Playlist
            Intent playerIntent = new Intent(mContext, PlayerActivity.class);
            playerIntent.putParcelableArrayListExtra("playlist", albumFiles);
            playerIntent.putExtra("position", position);
            playerIntent.putExtra("sender", "albumDetails");
            playerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(playerIntent);
        });
    }

    @Override
    public int getItemCount() {
        return albumFiles.size();
    }

    public class MyHolder extends RecyclerView.ViewHolder{
        ImageView album_image,menuMore;
        TextView album_name,artist_name,trackNumber;
        public MyHolder(@NonNull View itemView) {
            super(itemView);
            album_image = itemView.findViewById(R.id.music_img);
            menuMore = itemView.findViewById(R.id.menuMore);
            album_name = itemView.findViewById(R.id.music_file_name);
            artist_name = itemView.findViewById(R.id.artist_file_name);
            trackNumber = itemView.findViewById(R.id.music_track_number);
        }
    }


    void updateList(ArrayList<MusicFiles> newAlbumFiles) {
        // Ersetze die alte Liste komplett durch die neue.
        albumFiles = new ArrayList<>();
        albumFiles.addAll(newAlbumFiles);
        // Benachrichtige das RecyclerView, dass sich die Daten komplett geändert haben
        // und es sich neu zeichnen muss.
        notifyDataSetChanged();
    }

    private String getTrackNumber(String path) {
        try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
            retriever.setDataSource(path);
            // METADATA_KEY_CD_TRACK_NUMBER gibt oft "Track/Total" zurück, z.B. "7/12"
            String trackString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER);
            if (trackString != null) {
                // Wir nehmen nur den Teil vor dem "/"
                if (trackString.contains("/")) {
                    return trackString.substring(0, trackString.indexOf("/"));
                }
                return trackString;
            }
        } catch (Exception e) {
            Log.e("AlbumDetailsAdapter", "Fehler beim Lesen der Track-Nummer", e);
        }
        // Fallback, falls keine Track-Nummer gefunden wird
        return "•";
    }
}