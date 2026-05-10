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
import java.util.HashMap;
import java.util.Map;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.SearchHolder> {

    private final Context mContext;
    private final ArrayList<SearchResultItem> results;
    private Map<String, Integer> songPositionMap = new HashMap<>();

    public SearchResultAdapter(Context mContext, ArrayList<SearchResultItem> results, ArrayList<MusicFiles> allSongs) {
        this.mContext = mContext;
        this.results = results;
        for (int i = 0; i < allSongs.size(); i++) {
            songPositionMap.put(allSongs.get(i).getId(),i);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return results.get(position).type;
    }

    @NonNull
    @Override
    public SearchHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_search_result, parent, false);
        return new SearchHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchHolder holder, int position) {
        SearchResultItem item = results.get(position);
        MusicFiles song = item.musicFile;

        // Cover laden
        try {
            long albumId = Long.parseLong(song.getAlbumId());
            Uri artUri = ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"), albumId);
            Glide.with(mContext)
                    .load(artUri)
                    .placeholder(R.mipmap.ic_play)
                    .error(R.mipmap.ic_play)
                    .into(holder.image);
        } catch (Exception e) {
            Glide.with(mContext).load(R.mipmap.ic_play).into(holder.image);
        }

        switch (item.type) {
            case SearchResultItem.TYPE_SONG:
                holder.typeLabel.setText("🎵 Song");
                holder.title.setText(song.getTitle());
                holder.subtitle.setText(song.getArtist());
                holder.itemView.setOnClickListener(v -> {
                    Integer masterPosition = songPositionMap.get(song.getId());


                    if(masterPosition == null)return;

                    // Song direkt spielen
                    Intent serviceIntent = new Intent(mContext, MusicService.class);
                    serviceIntent.putExtra("actionName", "PLAY_MASTER_SONG");
                    serviceIntent.putExtra("position", masterPosition);
                    mContext.startService(serviceIntent);

                    Intent playerIntent = new Intent(mContext, PlayerActivity.class);
                    playerIntent.putExtra("position", masterPosition);
                    playerIntent.putExtra("sender", "search");
                    playerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(playerIntent);
                });
                break;

            case SearchResultItem.TYPE_ALBUM:
                holder.typeLabel.setText("💿 Album");
                holder.title.setText(song.getAlbum());
                holder.subtitle.setText(song.getArtist());
                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(mContext, AlbumDetails.class);
                    intent.putExtra("albumName", song.getAlbum());
                    mContext.startActivity(intent);
                });
                break;

            case SearchResultItem.TYPE_ARTIST:
                holder.typeLabel.setText("🎤 Artist");
                holder.title.setText(song.getArtist());
                holder.subtitle.setText("");
                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(mContext, ArtistActivity.class);
                    intent.putExtra("artistName", song.getArtist());
                    mContext.startActivity(intent);
                });
                break;
        }
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    public static class SearchHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView typeLabel, title, subtitle;

        public SearchHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.search_result_image);
            typeLabel = itemView.findViewById(R.id.search_result_type);
            title = itemView.findViewById(R.id.search_result_title);
            subtitle = itemView.findViewById(R.id.search_result_subtitle);
        }
    }
}
