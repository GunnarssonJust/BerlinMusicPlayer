package com.berlinmusicplayer;

import static com.berlinmusicplayer.MusicFiles.formatDuration;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
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

public class SongListAdapter extends RecyclerView.Adapter {
    private final Context mContext;
    private static final String TAG = "BINDUNGSHAFTUNG";
    ArrayList<MusicFiles> artistDetailFiles;
    int playlistIndex;
    private boolean showArtist;
    private final boolean isPlaylist;
    private static final int M_ADD = 1,M_REMOVE = 2,M_ALBUM = 3,M_ARTIST = 4,M_SLEEP_TIMER = 5;


    public SongListAdapter(Context mContext, ArrayList<MusicFiles> artistFiles, int playlistIndex, boolean showArtist, boolean isPlaylist) {
        this.mContext = mContext;
        artistDetailFiles = artistFiles;
        this.playlistIndex = playlistIndex;
        this.showArtist = showArtist;
        this.isPlaylist = isPlaylist;
        Log.d("ADAPTERTROTTEL", "Adapter erzeugt: " + this);
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
        Log.d(TAG,
                "Bind: " + song.getTitle() +
                        "  Position=" + position +
                        "  Current=" + isCurrentSong(song));

        myHolder.song_name.setText(String.format(song.getTitle()));
        myHolder.artist_name.setText(showArtist ? song.getArtist() : song.getAlbum()); // Zeige Album-Name statt Artist (ist ja klar wer der Artist in Artistdetails ist, sonst in Playlistdetails zeige artist.)
        myHolder.songduration.setText(formatDuration(song.getDuration()));
        // ← Prüfe ob das der aktuelle Song ist
        if (isCurrentSong(song)) {
            Log.d("BLAUESCHRIFT", "Song wird aktuell abgespielt: " + song.getTitle());
            myHolder.song_name.setTextColor(mContext.getColor(android.R.color.holo_blue_bright));
            myHolder.artist_name.setTextColor(mContext.getColor(android.R.color.holo_blue_bright));
            myHolder.songduration.setTextColor(mContext.getColor(android.R.color.holo_blue_bright));
        } else {
            Log.d("WEISSESCHRIFT", "Song ist aktuell BEENDET: " + song.getTitle());
            myHolder.song_name.setTextColor(mContext.getColor(android.R.color.white));
            myHolder.artist_name.setTextColor(mContext.getColor(android.R.color.darker_gray));
            myHolder.songduration.setTextColor(mContext.getColor(android.R.color.white));
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

            Menu menu = popupMenu.getMenu();
            menu.add(0, M_ADD, 0, "Hinzufügen");
            if(isPlaylist) menu.add(0, M_REMOVE, 1, "Aus Liste entfernen");
            menu.add(1, M_ALBUM, 2, "Album Details");
            if(showArtist) menu.add(1, M_ARTIST, 3, mContext.getString(R.string.interpret));
            menu.add(1, M_SLEEP_TIMER, 4, "Schlummermodus");


            popupMenu.setOnMenuItemClickListener(item -> {

                if(item.getItemId() == M_ALBUM){
                    Intent intent = new Intent(mContext, AlbumDetails.class);
                    intent.putExtra("albumName", song.getAlbum());
                    intent.putExtra("albumId", song.getAlbumId());
                    mContext.startActivity(intent);
                    return true;
                }else if(item.getItemId() == M_ARTIST){
                    Intent i = new Intent(mContext, ArtistActivity.class);
                    i.putExtra("artistName", song.getArtist());
                    mContext.startActivity(i);
                    return true;
                }else if(item.getItemId() == M_SLEEP_TIMER) {
                    MusicService svc = (mContext instanceof BaseActivity)
                            ? ((BaseActivity) mContext).getMusicService() : null;
                    SleepTimerDialog.show(mContext, svc);
                    return true;

                }else if(item.getItemId() == M_REMOVE){
                    new AlertDialog.Builder(mContext)
                            .setTitle("Song aus der Liste entfernen?")
                            .setMessage(song.getTitle() + " - " + song.getArtist())
                            .setPositiveButton("Ja", (dialog, which) -> {

                                PlaylistManager.getInstance(mContext)
                                        .removeSongFromPlaylist(playlistIndex, song.getId());
                                int pos = myHolder.getBindingAdapterPosition();
                                if (pos == RecyclerView.NO_POSITION) return;
                                artistDetailFiles.remove(pos);
                                notifyItemRemoved(pos);

                            })
                            .setNegativeButton("Nein", null)
                            .show();

                    return true;
                }else if(item.getItemId() == M_ADD) {

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
            int pos = myHolder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            // Service starten mit der Artist-Playlist
            Intent serviceIntent = new Intent(mContext, MusicService.class);
            serviceIntent.putExtra("actionName", "PLAY_ARTIST_SONG");
            if(isPlaylist) serviceIntent.putExtra("playlistIndex", playlistIndex);
            Log.d("PlaylistCover", "Klick: isPlaylist=" + isPlaylist + " playlistIndex=" + playlistIndex);
            serviceIntent.putParcelableArrayListExtra("artistSongs", artistDetailFiles);
            serviceIntent.putExtra("position", pos);
            if(isPlaylist){
                PlaylistManager.getInstance(mContext).setLastPlayedInPlaylist(playlistIndex, song.getId());
            }
            mContext.startService(serviceIntent);

            // PlayerActivity öffnen
            Intent playerIntent = new Intent(mContext, PlayerActivity.class);
            playerIntent.putExtra("sender", "artistDetails");
            playerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            mContext.startActivity(playerIntent);
        });
    }

    @Override
    public int getItemCount() {
        return artistDetailFiles != null ? artistDetailFiles.size() : 0;
    }

    public static class MyHolder extends RecyclerView.ViewHolder {
        ImageView album_image;
        TextView song_name, artist_name,songduration;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            album_image = itemView.findViewById(R.id.music_img);
            song_name = itemView.findViewById(R.id.music_file_name);
            artist_name = itemView.findViewById(R.id.artist_file_name);
            songduration = itemView.findViewById(R.id.song_duration);
        }
    }

    void updateList(ArrayList<MusicFiles> newFiles) {
        artistDetailFiles = new ArrayList<>(newFiles);
        notifyDataSetChanged();
        updateCurrentSong();
    }
    private boolean isCurrentSong(MusicFiles song) {
        SharedPreferences prefs = mContext.getSharedPreferences("LAST_PLAYED", Context.MODE_PRIVATE);
        String currentSongId = prefs.getString("current_song_id", "");
        Log.d("IDCHECK",
                "Song=" + song.getTitle()
                        + " songId=" + song.getId()
                        + " currentId=" + currentSongId);
        return currentSongId.equals(song.getId());
    }

    public void updateCurrentSong() {
        notifyItemRangeChanged(0, getItemCount());  // ← Alle Items neu binden!
    }
}

