package com.berlinmusicplayer;

import static com.berlinmusicplayer.MusicService.ARTIST_NAME;
import static com.berlinmusicplayer.MusicService.MUSIC_FILE;
import static com.berlinmusicplayer.MusicService.MUSIC_LAST_PLAYED;
import static com.berlinmusicplayer.MusicService.SONG_NAME;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener, ServiceConnection, MusicServiceCallback {

    public static final String TAG = "MainActivity";
    public static final int REQUEST_CODE = 1;
    ArrayList<MusicFiles> musicFiles = new ArrayList<>();
    ArrayList<MusicFiles> albums = new ArrayList<>();
    ArrayList<MusicFiles> artist = new ArrayList<>();
    public static String MY_SORT_PREF = "SortOrder";
    private boolean D = true;
    private ViewPagerAdapter viewPagerAdapter;
    public MusicService musicService;
    private SongsFragment songsFragment;
    private AlbumFragment albumFragment;
    private ArtistFragment artistFragment;

    // NEU
    PlaylistFragment playlistFragment;
    // ← NEU
    RecentFragment recentFragment;
    private ViewPager2 viewPager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setOverflowIcon(ContextCompat.getDrawable(this, R.drawable.ic_gate_white));
        initViewPager();
        permission();

        Intent serviceIntent = new Intent(this, MusicService.class);
    
		// Prüfe, ob ein letzter Song existiert
        if(savedInstanceState == null) {
            SharedPreferences prefs = getSharedPreferences(MusicService.MUSIC_LAST_PLAYED, MODE_PRIVATE);
            String lastMusicFile = prefs.getString(MusicService.MUSIC_FILE, null);

            if (savedInstanceState == null && musicService == null) {
                // Nur beim ersten Start der App!
                if (lastMusicFile != null) {
                    Intent restoreIntent = new Intent(this, MusicService.class);
                    restoreIntent.putExtra("actionName","RESTORE_LAST_SONG");
                    startService(restoreIntent);
                }
            }
        }
		
		// Service starten (damit onStartCommand() aufgerufen wird)
		startService(serviceIntent);
		
		// UND binden (für die Kommunikation)
		bindService(serviceIntent, this, BIND_AUTO_CREATE);
		// ==================================================
		
		handleIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        showLastPlayedSongInFragment();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        if (D) Log.d(TAG, "Esel MainActivity: onServiceConnected");
        MusicService.MyBinder binder = (MusicService.MyBinder) service;
        musicService = binder.getService();
        musicService.setMainActivityCallback(this);
        musicFiles = musicService.getMusicFiles();
        PlaylistManager.getInstance(this).setMusicFiles(musicFiles);
        if(songsFragment != null){
            songsFragment.refreshList();
        }
        initViewPagerAndAlbums();

        // 4. Informiere das bereits existierende SongsFragment, dass es neue Daten gibt.
        if (songsFragment != null) {
            songsFragment.setMusicList(musicFiles);
            Log.d(TAG, "SongsFragment wurde mit " + musicFiles.size() + " Songs aktualisiert.");
        }
        if (albumFragment != null) { // <-- NEU
            albumFragment.updateAdapterList(albums);
            Log.d(TAG, "AlbumFragment wurde mit " + albums.size() + " Alben aktualisiert.");
        }
        if (artistFragment != null) { // <-- NEU
            artistFragment.updateAdapterList(artist);
            Log.d(TAG, "ArtistFragment wurde mit " + artist.size() + " Künstlern aktualisiert.");
            // ---------------------------------------------
        }
        // 5. Aktualisiere den Mini-Player mit dem aktuellen Zustand.
        if (musicService.isPlaying()) {
            onSongChanged();
        } else {
            showLastPlayedSongInFragment();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        musicService = null;
    }

    @Override
    public void onSongChanged() {
        runOnUiThread(() -> {
            NowPlayingFragmentBottom bottomPlayer = findBottomPlayerFragment();
            if (bottomPlayer != null && musicService != null && musicService.position != -1) {
                ArrayList<MusicFiles> currentPlaylist = musicService.getCurrentPlaylist();
                if(currentPlaylist != null && musicService.position < currentPlaylist.size()){
                    MusicFiles currentSong = currentPlaylist.get(musicService.position);
                    bottomPlayer.updateNowPlayingUI(currentSong, musicService.isPlaying());
                }
                // ------------------------------------
            }
        });
    }

    @Override
    public void onPlaybackStateChanged() {
        runOnUiThread(() -> {
            NowPlayingFragmentBottom bottomPlayer = findBottomPlayerFragment();
            if (bottomPlayer != null && musicService != null && musicService.position != -1) {
                // --- HIER IST DIE FINALE KORREKTUR ---
                ArrayList<MusicFiles> currentPlaylist = musicService.getCurrentPlaylist();
                if(currentPlaylist != null && musicService.position < currentPlaylist.size()){
                    MusicFiles currentSong = currentPlaylist.get(musicService.position);
                    bottomPlayer.updateNowPlayingUI(currentSong, musicService.isPlaying());
                }
            }
            if(songsFragment!=null)songsFragment.refreshAdapter();
            if(albumFragment!= null) albumFragment.refreshAdapter();
            if(artistFragment!=null)artistFragment.refreshAdapter();
        });
    }

    private NowPlayingFragmentBottom findBottomPlayerFragment() {
        return (NowPlayingFragmentBottom) getSupportFragmentManager().findFragmentById(R.id.frag_bottom_player);
    }

    private void showLastPlayedSongInFragment() {NowPlayingFragmentBottom bottomPlayerFragment = findBottomPlayerFragment();
        if (bottomPlayerFragment == null || bottomPlayerFragment.getView() == null) {
            return;
        }

        // FALL A: Service ist verbunden und hat die Kontrolle.
        if (musicService != null) {
            ArrayList<MusicFiles> currentPlaylist = musicService.getCurrentPlaylist();
            int currentPosition = musicService.getPosition();

            if (currentPlaylist != null && !currentPlaylist.isEmpty() && currentPosition != -1 && currentPosition < currentPlaylist.size()) {
                MusicFiles currentSong = currentPlaylist.get(currentPosition);
                bottomPlayerFragment.updateNowPlayingUI(currentSong, musicService.isPlaying());
                bottomPlayerFragment.getView().setVisibility(View.VISIBLE);
            } else {
                bottomPlayerFragment.getView().setVisibility(View.GONE);
            }
        }
        // FALL B: Service ist noch NICHT verbunden. Lese aus SharedPreferences als Fallback.
        else {
            SharedPreferences preferences = getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE);
            String path = preferences.getString(MUSIC_FILE, null);
            if (path != null) {
                String artist = preferences.getString(ARTIST_NAME, "");
                String songTitle = preferences.getString(SONG_NAME, "");
                MusicFiles lastPlayedFile = new MusicFiles(path, songTitle, artist, "", "", "", "","",0,0);
                bottomPlayerFragment.updateNowPlayingUI(lastPlayedFile, false);
                bottomPlayerFragment.getView().setVisibility(View.VISIBLE);
            } else {
                bottomPlayerFragment.getView().setVisibility(View.GONE);
            }
        }
    }

    public MusicService getMusicService() {
        return musicService;
    }

    private void permission() {
        ArrayList<String> permissionsToRequest = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO);
            }
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(MainActivity.this, permissionsToRequest.toArray(new String[0]), REQUEST_CODE);
        } else {
            initViewPagerAndAlbums();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            if (grantResults.length > 0) {
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        allPermissionsGranted = false;
                        break;
                    }
                }
            } else {
                allPermissionsGranted = false;
            }

            if (allPermissionsGranted) {
                recreate();
            } else {
                Toast.makeText(this, "Berechtigung verweigert. Die App kann keine Musik laden.", Toast.LENGTH_LONG).show();
                // Lade die UI trotzdem, sie wird leer sein.
                initViewPagerAndAlbums();
            }
        }
    }

    private void initViewPager() {
        viewPager = findViewById(R.id.viewpager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        viewPagerAdapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(viewPagerAdapter);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> tab.setText(viewPagerAdapter.getPageTitle(position))).attach();
    }

    public static class ViewPagerAdapter extends FragmentStateAdapter {
        private MainActivity mainActivity;

        public ViewPagerAdapter(@NonNull MainActivity mainActivity) {
            super(mainActivity);
            this.mainActivity = mainActivity;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    if (mainActivity.songsFragment == null) {
                        mainActivity.songsFragment = new SongsFragment();
                    }
                    return mainActivity.songsFragment;
                case 1:
                    if (mainActivity.albumFragment == null) {
                        mainActivity.albumFragment = new AlbumFragment();
                    }
                    return mainActivity.albumFragment;
                case 2:
                    if (mainActivity.artistFragment == null) {
                        mainActivity.artistFragment = new ArtistFragment();
                    }
                    return mainActivity.artistFragment;
                case 3:
                    if (mainActivity.playlistFragment == null) {
                        mainActivity.playlistFragment = new PlaylistFragment();
                    }
                    return mainActivity.playlistFragment;
                case 4:
                    if (mainActivity.recentFragment == null) {
                        mainActivity.recentFragment = new RecentFragment();
                    }
                    return mainActivity.recentFragment;
                default:
                    return new SongsFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 5;
        }

        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Titel";
                case 1:
                    return "Alben";
                case 2:
                    return "Künstler";
                case 3:
                    return "Playlists";
                case 4:
                    return "Zuletzt";
                default:
                    return "Titel";
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);
        MenuItem menuItem = menu.findItem(R.id.search_option);
        menuItem.setOnMenuItemClickListener(item -> {
            // SearchActivity öffnen statt inline suchen
            Intent intent = new Intent(this, SearchActivity.class);
            startActivity(intent);
            return true;
        });
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        String userInput = newText.toLowerCase();
        ArrayList<MusicFiles> myFiles = new ArrayList<>();
        if (musicFiles != null) {
            for (MusicFiles song : musicFiles) {
                if (song.getTitle().toLowerCase().contains(userInput)) {
                    myFiles.add(song);
                }
            }
        }
        if (songsFragment != null) {
            songsFragment.updateAdapterList(myFiles,true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        // 1. Präferenz speichern
        SharedPreferences.Editor editor = getSharedPreferences(MY_SORT_PREF, MODE_PRIVATE).edit();
        if (itemId == R.id.by_name) editor.putString("sorting", "sortByName");
        else if (itemId == R.id.by_date) editor.putString("sorting", "sortByDate");
        else if (itemId == R.id.by_size) editor.putString("sorting", "sortBySize");
        editor.apply();

        // 2. Neu laden im Background — kein recreate() mehr!
        new Thread(() -> {
            // ← Neue Liste erstellen, NICHT die alte leeren!
            ArrayList<MusicFiles> sorted = getAllAudio();

            runOnUiThread(() -> {
                // ← Erst alles updaten, dann Service informieren
                musicFiles = sorted;  // ← Atomisch ersetzen!

                // Service auch updaten!
                if (musicService != null) {
                    musicService.setMusicFiles(musicFiles);  // ← NEU!
                }

                if (songsFragment != null) songsFragment.setMusicList(musicFiles);
                if (albumFragment != null) albumFragment.setAlbumList(musicFiles);
                if (artistFragment != null) artistFragment.setArtistList(musicFiles);
            });
        }).start();

        return true;
    }

    private void initViewPagerAndAlbums() {
        // Initialisiere den ViewPager nur einmalig. Am besten in onCreate nach permission().
        // initViewPager(); // Diesen Aufruf hier entfernen.

        // Diese Methode hat jetzt nur noch EINE Aufgabe: die Alben- und Künstlerliste vorzubereiten.
        if (musicFiles != null && !musicFiles.isEmpty()) {
            albums.clear();
            artist.clear(); // Auch die Künstlerliste leeren
            ArrayList<String> duplicateAlbumNames = new ArrayList<>();
            ArrayList<String> duplicateArtistNames = new ArrayList<>();

            for (MusicFiles song : musicFiles) {
                String uniqueKey = song.getAlbum() + "_" + song.getAlbumId();
                // Alben-Liste erstellen, auch wenn es doppelte Einträge(z.B. Album: Solo,
                // Künstler: Louane & Peter Heppner) gibt
                if (!duplicateAlbumNames.contains(uniqueKey)) {
                    albums.add(song);
                    duplicateAlbumNames.add(uniqueKey);
                }
                // Künstler-Liste erstellen
                if (!duplicateArtistNames.contains(song.getArtist())) {
                    artist.add(song);
                    duplicateArtistNames.add(song.getArtist());
                }
            }
            Collections.sort(albums, (a, b) -> a.getAlbum().compareToIgnoreCase(b.getAlbum()));
            Collections.sort(artist, (a, b) -> a.getArtist().compareToIgnoreCase(b.getArtist()));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }
    public ArrayList<MusicFiles> getAllAudio() {
        return musicFiles;
    }
    public ArrayList<MusicFiles> getAllAlbum() {
        return albums;
    }
    public ArrayList<MusicFiles> getAllArtist() {
        return artist;
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.hasExtra("targetFragment")) {
            String target = intent.getStringExtra("targetFragment");
            if("SongsFragment".equals(target)) {
                if(viewPager != null){
                    viewPager.setCurrentItem(0, false);
                }
            }
        }
    }
    public int findSongPositionByPath(String path) {
        if (path == null || musicFiles == null) return -1;

        for (int i = 0; i < musicFiles.size(); i++) {
            if (path.equals(musicFiles.get(i).getPath())) {
                return i;
            }
        }
        return -1;
    }
    protected void onDestroy(){
        super.onDestroy();
    }
}
      