package com.berlinmusicplayer;

public class SearchResultItem {
    public static final int TYPE_SONG = 0;
    public static final int TYPE_ALBUM = 1;
    public static final int TYPE_ARTIST = 2;

    public int type;
    public MusicFiles musicFile;

    public SearchResultItem(int type, MusicFiles musicFile) {
        this.type = type;
        this.musicFile = musicFile;
    }
}
