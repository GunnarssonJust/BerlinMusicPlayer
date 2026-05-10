package com.berlinmusicplayer;


import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.Collections;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.util.ArrayList;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.MyHolder>{
    private Context mContext;
    private ArrayList<MusicFiles> albumFiles;
    View view;
    public AlbumAdapter(Context mContext, ArrayList<MusicFiles> albumFiles) {
        this.mContext = mContext;
        this.albumFiles = albumFiles;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        view = LayoutInflater.from(mContext).inflate(R.layout.album_item,parent,false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, final int position) {
        holder.album_name.setText(albumFiles.get(position).getAlbum());

        Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
        String albumId = albumFiles.get(position).getAlbumId();
         Glide.with(mContext)
                    .load(ContentUris.withAppendedId(sArtworkUri,Long.parseLong(albumId)))
                    .placeholder(R.drawable.ic_play_btn)
                    .error(R.drawable.ic_play_btn)
                    .into(holder.album_image);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, AlbumDetails.class);
            intent.putExtra("albumName",albumFiles.get(position).getAlbum());
            mContext.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return albumFiles.size();
    }

    public class MyHolder extends RecyclerView.ViewHolder{
        ImageView album_image;
        TextView album_name;
        public MyHolder(@NonNull View itemView) {
            super(itemView);
            album_image = itemView.findViewById(R.id.albumitem_image);
            album_name = itemView.findViewById(R.id.album_subtext);
        }
    }

    void updateList(ArrayList<MusicFiles> albumFilesArrayList){
        albumFiles = new ArrayList<>(albumFilesArrayList);
        notifyDataSetChanged();
    }
}