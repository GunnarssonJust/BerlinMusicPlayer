package com.berlinmusicplayer;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class PlaylistManager {

    private static final String PREFS_NAME = "BerlinMusicPlayerPrefs";
    private static final String KEY_PLAYLISTS = "playlists";
    private static final String KEY_RECENT = "recentSongs";
    private static final int MAX_RECENT = 100;

    private ArrayList<MusicFiles> musicFiles;
    private Map<String, MusicFiles> musicMap = new HashMap<>();

    private static PlaylistManager instance;
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    // ─── Singleton ────────────────────────────────────────────────
    public static PlaylistManager getInstance(Context context) {
        if (instance == null) {
            instance = new PlaylistManager(context.getApplicationContext());
        }
        return instance;
    }
    public void setMusicFiles(ArrayList<MusicFiles> musicFiles) {
        this.musicFiles = musicFiles;
        musicMap.clear();

        for (MusicFiles song : musicFiles) {
            musicMap.put(song.getId(), song);
        }
    }

    private PlaylistManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ─── Playlist Model ───────────────────────────────────────────
    public static class Playlist {
        public String name;
        public ArrayList<String> songIds; // Song-IDs

        public Playlist(String name) {
            this.name = name;
            this.songIds = new ArrayList<>();
        }
    }

    // ─── Playlists laden/speichern ────────────────────────────────
    public ArrayList<Playlist> getPlaylists() {
        String json = prefs.getString(KEY_PLAYLISTS, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<ArrayList<Playlist>>() {}.getType();
        ArrayList<Playlist> list = gson.fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }

    private void savePlaylists(ArrayList<Playlist> playlists) {
        prefs.edit().putString(KEY_PLAYLISTS, gson.toJson(playlists)).apply();
    }

    // ─── Playlist erstellen ───────────────────────────────────────
    public void createPlaylist(String name) {
        ArrayList<Playlist> playlists = getPlaylists();
        playlists.add(new Playlist(name));
        savePlaylists(playlists);
    }

    // ─── Playlist umbenennen ──────────────────────────────────────
    public void renamePlaylist(int index, String newName) {
        ArrayList<Playlist> playlists = getPlaylists();
        if (index >= 0 && index < playlists.size()) {
            playlists.get(index).name = newName;
            savePlaylists(playlists);
        }
    }

    // ─── Playlist löschen ─────────────────────────────────────────
    public void deletePlaylist(int index) {
        ArrayList<Playlist> playlists = getPlaylists();
        if (index >= 0 && index < playlists.size()) {
            playlists.remove(index);
            savePlaylists(playlists);
        }
    }

    // ─── Song zur Playlist hinzufügen ─────────────────────────────
    public void addSongToPlaylist(int playlistIndex, String songId) {
        ArrayList<Playlist> playlists = getPlaylists();
        if (playlistIndex >= 0 && playlistIndex < playlists.size()) {
            Playlist playlist = playlists.get(playlistIndex);
            if (!playlist.songIds.contains(songId)) {
                playlist.songIds.add(songId);
                savePlaylists(playlists);
            }
        }
    }

    // ─── Song aus Playlist entfernen ──────────────────────────────
    public void removeSongFromPlaylist(int playlistIndex, String songId) {
        ArrayList<Playlist> playlists = getPlaylists();
        if (playlistIndex >= 0 && playlistIndex < playlists.size()) {
            playlists.get(playlistIndex).songIds.remove(songId);
            savePlaylists(playlists);
        }
    }

    // ─── Songs einer Playlist aus musicFiles holen ────────────────
    public ArrayList<MusicFiles> getSongsForPlaylist(int playlistIndex) {
        ArrayList<Playlist> playlists = getPlaylists();
        if (playlistIndex < 0 || playlistIndex >= playlists.size()) return new ArrayList<>();

        ArrayList<String> ids = playlists.get(playlistIndex).songIds;
        ArrayList<MusicFiles> songs = new ArrayList<>();

        if (musicFiles == null) return songs;

        for (String id : ids) {
            MusicFiles song = musicMap.get(id);
            if (song != null) {
                songs.add(song);
            }
        }
        return songs;
    }

    // ─── Zuletzt gespielt ─────────────────────────────────────────
    public void addToRecent(String songId) {
        LinkedList<String> recent = getRecentIds();
        recent.remove(songId); // Doppelte entfernen
        recent.addFirst(songId); // Vorne einfügen
        while (recent.size() > MAX_RECENT) recent.removeLast(); // Max 100
        prefs.edit().putString(KEY_RECENT, gson.toJson(recent)).apply();
    }

    public LinkedList<String> getRecentIds() {
        String json = prefs.getString(KEY_RECENT, null);
        if (json == null) return new LinkedList<>();
        Type type = new TypeToken<LinkedList<String>>() {}.getType();
        LinkedList<String> list = gson.fromJson(json, type);
        return list != null ? list : new LinkedList<>();
    }

    public ArrayList<MusicFiles> getRecentSongs() {
        LinkedList<String> ids = getRecentIds();
        ArrayList<MusicFiles> songs = new ArrayList<>();
        if (musicFiles == null) return songs;

        for (String id : ids) {
            MusicFiles song = musicMap.get(id);
            if (song != null) {
                songs.add(song);
            }
        }
        return songs;
    }
    public ArrayList<MusicFiles> getMusicFiles(){
        return musicFiles;
    }
}
