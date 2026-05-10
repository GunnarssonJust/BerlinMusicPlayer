package com.berlinmusicplayer;

import android.icu.text.CaseMap;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

public class MusicFiles implements Parcelable {
    private String path;
    private String title;
    private String artist;
    private String album;
    private String duration;
    private String id;
    private String albumId;

    public MusicFiles(String path, String title, String artist, String album, String duration, String id, String albumId) {
        this.path = path;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
        this.id = id;
        this.albumId = albumId;
    }

    public MusicFiles() {
    }
    public String getId(){
        return id;
    }
    public void setId(){
        this.id = id;
    }
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getAlbumId() {
        return albumId;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(path);
        dest.writeString(title);
        dest.writeString(artist);
        dest.writeString(album);
        dest.writeString(duration);
        dest.writeString(id);
        dest.writeString(albumId);
    }

    protected MusicFiles(Parcel in){
        path = in.readString();
        title = in.readString();
        artist = in.readString();
        album = in.readString();
        duration = in.readString();
        id = in.readString();
        albumId = in.readString();
    }
    public static final Creator<MusicFiles> CREATOR = new Creator<MusicFiles>() {
        @Override
        public MusicFiles createFromParcel(Parcel in) {
            return new MusicFiles(in);
        }

        @Override
        public MusicFiles[] newArray(int size) {
            return new MusicFiles[size];
        }
    };
}
