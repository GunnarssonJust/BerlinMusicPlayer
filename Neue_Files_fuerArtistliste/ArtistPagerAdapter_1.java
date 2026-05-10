package com.berlinmusicplayer;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;

public class ArtistPagerAdapter extends FragmentStateAdapter {

    private final String artistName;
    private final ArrayList<MusicFiles> artistSongs;

    public ArtistPagerAdapter(@NonNull FragmentActivity fragmentActivity,
                               String artistName,
                               ArrayList<MusicFiles> artistSongs) {
        super(fragmentActivity);
        this.artistName = artistName;
        this.artistSongs = artistSongs;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return ArtistAlbumsFragment.newInstance(artistName);
            case 1:
                return ArtistSongsFragment.newInstance(artistName, artistSongs);
            default:
                return ArtistAlbumsFragment.newInstance(artistName);
        }
    }

    @Override
    public int getItemCount() {
        return 2; // Alben + Titel
    }
}
