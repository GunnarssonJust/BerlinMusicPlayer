package com.berlinmusicplayer;

import static com.berlinmusicplayer.MainActivity.MY_SORT_PREF;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class PlayerActivity extends AppCompatActivity implements ActionPlaying, ServiceConnection, SearchView.OnQueryTextListener {

    private final boolean D = true;
    private static final String TAG = "PlayerActivity";
    TextView song_name, artist, duration_played, duration_total;
    ImageView cover_art, nextBtn, prevBtn, shufflebtn, repeatBtn, playPauseBtn,playListButton;
    ConstraintLayout mContainer;
    SeekBar seekBar;
    Toolbar toolbar;
    private int position = -1;
    private ArrayList<MusicFiles> listSongs = new ArrayList<>();
    private Uri uri;
    private final Handler handler = new Handler();
    //private Thread playThread, prevThread, nextThread;
    private MusicService musicService;
    Bitmap bitmap;
    //public static final String poS = "POSITION";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (D) Log.d(TAG, "Esel PlayerActivity: onCreate");
        setContentView(R.layout.activity_player);
        initViews();
        getIntentMethod();
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (musicService != null && fromUser) {
                    musicService.seekTo(progress * 1000);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        toolbar = findViewById(R.id.player_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setOverflowIcon(ContextCompat.getDrawable(this, R.drawable.ic_gate));
        mContainer = findViewById(R.id.mContainer);
        mContainer.setOnTouchListener(new View.OnTouchListener(){
            private float startY;
            private static final int SWIPE_THRESHOLD = 150;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        startY = event.getY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        float deltaY = event.getY() - startY;
                        if(Math.abs(deltaY) > SWIPE_THRESHOLD){
                            goToMainActivity();
                        }
                        return true;
                }
                return false;
            }
        });

        PlayerActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (musicService != null && musicService.isPlaying()) {
                    int mCurrentPositionMillis = musicService.getCurrentPosition();
                    int mCurrentPositionSeconds = mCurrentPositionMillis / 1000;
                    seekBar.setProgress(mCurrentPositionSeconds);
                    duration_played.setText(formattedTime(mCurrentPositionSeconds));
                }
                handler.postDelayed(this, 1000);
            }
        });

        shufflebtn.setOnClickListener(v -> {
            if (musicService != null) {
                musicService.shuffleBtnClicked();
            }
        });
        repeatBtn.setOnClickListener(view -> {
            if (musicService != null) {
                musicService.repeatBtnClicked();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (D) Log.d(TAG, "Esel PlayerActivity: onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.player_menu, menu);
        MenuItem menuItem = menu.findItem(R.id.search_option);
        if (menuItem != null) {
            menuItem.setOnMenuItemClickListener(item -> {
                Intent intent = new Intent(this, SearchActivity.class);
                startActivity(intent);
                return true;
            });
        }
        return true;
    }

    private int getPosition() {
        return position;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (D) Log.d(TAG, "Esel PlayerActivity: onResume");
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, this, BIND_AUTO_CREATE);
        if(musicService != null)
            updateUiComponents();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(this);
    }

    @Override
    public void prevBtnClicked() {
        if (D) Log.d(TAG, "-> prevBtnClicked");
        runOnUiThread(this::updateUiComponents);
    }

    @Override
    public void updatePlayerView() {
        if (D) Log.d(TAG, "--> UI-Update vom Service empfangen");
        // UI-Updates müssen auf dem Haupt-Thread laufen.
        runOnUiThread(this::updateUiComponents);
    }


    public void nextBtnClicked() {
        if (D) Log.d(TAG, "--> nextBtnClicked");
        runOnUiThread(this::updateUiComponents);
    }

    private int getRandom(int size) {
        if (D) Log.d(TAG, "Esel PlayerActivity: getRandom");
        return new Random().nextInt(size);
    }

    public void onPlayPauseBtnClicked() {
        if (D) Log.d(TAG, "Esel PlayerActivity: playPauseBtnClicked");
        if (musicService != null) {
            musicService.playPauseBtnClicked();
        }
    }

    private String formattedTime(int time) {
        if (D) Log.d(TAG, "Esel PlayerActivity: formattedTime");
        String totalout = "";
        String totalNew = "";
        String seconds = String.valueOf(time % 60);
        String minutes = String.valueOf(time / 60);
        totalout = minutes + ":" + seconds;
        totalNew = minutes + ":" + "0" + seconds;
        if (seconds.length() == 1) {
            return totalNew;
        } else {
            return totalout;
        }
    }

    private void getIntentMethod() {
        if (D) Log.d(TAG, "Esel PlayerActivity: getIntentMode");
        position = getIntent().getIntExtra("position", -1);
        String sender = getIntent().getStringExtra("sender");

        if ("albumDetails".equals(sender)) {
            ArrayList<MusicFiles> albumSongs = getIntent().getParcelableArrayListExtra("albumSongs");
            if (albumSongs != null) {
                this.listSongs = albumSongs;
            }
        }
    }

    private void initViews() {
        if (D) Log.d(TAG, "Esel PlayerActivity: initViews");
        song_name = findViewById(R.id.songTitle);
        song_name.setSelected(true);// für marqee_forever notwendig!
        artist = findViewById(R.id.songArtist);
        artist.setSelected(true);// für marqee_forever notwendig!
        duration_played = findViewById(R.id.songCurrentDurationLabel);
        duration_total = findViewById(R.id.songRemainingDurationLabel);
        cover_art = findViewById(R.id.album_image);

        nextBtn = findViewById(R.id.forward);
        prevBtn = findViewById(R.id.rewind);
        shufflebtn = findViewById(R.id.btn_shuffle);
        playListButton = findViewById(R.id.btn_add_2_playlist);
        repeatBtn = findViewById(R.id.btn_repeat);
        playPauseBtn = findViewById(R.id.play_pause);
        seekBar = findViewById(R.id.seekBar);
    }

    private void updateUiComponents() {
        if (musicService == null) return;

        int currentPosition = musicService.getPosition();
        ArrayList<MusicFiles> currentPlaylist = musicService.getCurrentPlaylist(); // Oder wie auch immer die Liste im Service heißt

        if (currentPlaylist != null && !currentPlaylist.isEmpty() && currentPosition >= 0 && currentPosition < currentPlaylist.size()) {
            MusicFiles currentSong = currentPlaylist.get(currentPosition);
            song_name.setText(currentSong.getTitle());
            artist.setText(currentSong.getArtist());
            metaData(Uri.parse(currentSong.getPath()));
        }
        updatePlayPauseButton();
    }

    void metaData(Uri uri) {
        if (D) Log.d(TAG, "Esel PlayerActivity: metaData() für URI: " + uri.toString());
        if(isDestroyed()){
            return;
        }
        byte[] art = null;
        if (uri == null) {
            Log.e(TAG, "metaData: uri ist null, Standardbild wird geladen");
            Glide.with(this)
                    .asBitmap()
                    .load(R.mipmap.ic_play_pressed)
                    .into(cover_art);
            return;
        }
        try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
            retriever.setDataSource(uri.toString());
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (durationStr != null) {
                int durationMillis = Integer.parseInt(durationStr);
                int durationSeconds = durationMillis / 1000;
                duration_total.setText(formattedTime(durationSeconds));
                seekBar.setMax(durationSeconds);
                //final int durationTotal = Integer.parseInt(listSongs.get(position).getDuration()) / 1000;
            }
            //Album-Cover Import aus den Metadaten des Songs
            art = retriever.getEmbeddedPicture();
        } catch (Exception e) {
            Log.e(TAG, "metaData: Fehler beim Lesen der Metadaten: Metadata() für URI: " + uri.toString() + " ", e);
        }

        if (art != null && art.length > 0) {
            bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
            ImageAnimation(this, cover_art, bitmap);
        } else {
            Glide.with(this)
                    .asBitmap()
                    .load(R.mipmap.ic_play_pressed)
                    .into(cover_art);

        }
        ConstraintLayout mContainer = findViewById(R.id.mContainer);
        mContainer.setBackgroundResource(R.drawable.bg_player);
        song_name.setTextColor(getColor(R.color.lightred));
        artist.setTextColor(getColor(R.color.lightgreen));
        duration_played.setTextColor(getColor(R.color.lightred));
        duration_total.setTextColor(getColor(R.color.lightgreen));
    }

    public void ImageAnimation(Context context, ImageView imageView, Bitmap bitmap) {
        if (D) Log.d(TAG, "Esel PlayerActivity: ImageAnimation");
        Animation animOut = AnimationUtils.loadAnimation(context, android.R.anim.fade_out);
        Animation animIn = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
        animOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Glide.with(context).load(bitmap).into(imageView);
                animIn.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                imageView.startAnimation(animIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        imageView.startAnimation(animOut);
    }

    // In PlayerActivity.java

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        if (D) Log.d(TAG, "Esel PlayerActivity: onServiceConnected");

        // 1. Verbindung zum Service herstellen und Callback setzen
        MusicService.MyBinder myBinder = (MusicService.MyBinder) service;
        musicService = myBinder.getService();
        musicService.setCallBack(this);
        updateUiComponents();
        // 2. Prüfen, ob ein Startbefehl vom Intent vorliegt
        int startPosition = getIntent().getIntExtra("position", -1);

        if (startPosition != -1) {
            // --- FALL A: EIN NEUER SONG SOLL GESTARTET WERDEN ---
            Log.d(TAG, "onServiceConnected: Befehl zum Starten eines neuen Songs an Position " + startPosition + " erhalten.");

            // Hole die Playlist aus dem Intent, falls eine mitgegeben wurde (von AlbumDetails, Suche etc.)
            ArrayList<MusicFiles> newPlaylist = getIntent().getParcelableArrayListExtra("playlist");

            if (newPlaylist == null) {
                newPlaylist = getIntent().getParcelableArrayListExtra("albumSongs");
            }

            if (newPlaylist != null && !newPlaylist.isEmpty()) {
                // Wir haben eine spezifische Playlist (z.B. ein Album). Gib sie dem Service.
                musicService.playNewPlayList(newPlaylist, startPosition);
            } else {
                // Keine spezifische Playlist wurde mitgegeben (z.B. Klick aus der Hauptliste).
                // Befiehl dem Service, seine Master-Liste zu verwenden.
                musicService.playNewPlayList(musicService.getMusicFiles(), startPosition);
            }

            // WICHTIG: "Verbrauche" die Position, damit sie beim nächsten onResume (z.B. nach Bildschirmdrehung) nicht wieder ausgelöst wird.
            getIntent().removeExtra("playlist");
            getIntent().removeExtra("position");

        } else {
            // --- FALL B: KEIN NEUER SONG ANGEFORDERT (z.B. bei Rückkehr zur Activity) ---
            // Synchronisiere die UI einfach mit dem, was gerade im Service läuft.
            Log.d(TAG, "onServiceConnected: Kein neuer Song, synchronisiere UI mit laufendem Zustand.");
            updateUiComponents();
        }

        // 3. UI-Listener setzen (wird sicherheitshalber hier gemacht, nachdem der Service verbunden ist)
        playPauseBtn.setOnClickListener(view -> onPlayPauseBtnClicked());
        nextBtn.setOnClickListener(view -> {
            if (musicService != null) musicService.nextBtnClicked();
        });
        prevBtn.setOnClickListener(view -> {
            if (musicService != null) musicService.previousBtnClicked();
        });

        // 4. Initialisiere den SeekBar-Updater-Thread
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (musicService != null && musicService.isPlaying()) {
                    int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                    seekBar.setProgress(mCurrentPosition);
                    duration_played.setText(formattedTime(mCurrentPosition));
                }
                // Lasse den Handler weiterlaufen, um die SeekBar zu aktualisieren
                handler.postDelayed(this, 1000);
            }
        });

        // 5. Initialen Zustand der Shuffle/Repeat-Buttons setzen
        if (musicService != null) {
            shufflebtn.setImageResource(musicService.getShuffleState() ? R.drawable.ic_shuffle_green : R.drawable.ic_shuffle);
            repeatBtn.setImageResource(musicService.getRepeatState() ? R.drawable.ic_repeat_red : R.drawable.ic_repeat);
        }


        // 4. Setze die UI-Listener. Dies sollte nur einmal passieren.
        playPauseBtn.setOnClickListener(view -> onPlayPauseBtnClicked());
        nextBtn.setOnClickListener(view -> {
            if (musicService != null) {
                musicService.nextBtnClicked();
            }
        });
        prevBtn.setOnClickListener(view -> {
            if (musicService != null) {
                musicService.previousBtnClicked();
            }
        });
        playListButton.setOnClickListener(view -> {
            if(musicService == null) return;
            PlaylistManager manager = PlaylistManager.getInstance(this);
            ArrayList<PlaylistManager.Playlist> playlists = manager.getPlaylists();
            if(playlists.isEmpty()){
                Toast.makeText(this,"Keine Playlists vorhanden!",Toast.LENGTH_SHORT).show();
            }
            String[] names = new String[playlists.size()];
            for (int i = 0; i < playlists.size(); i++) {
                names[i] = playlists.get(i).name;
            }

            // Aktuellen Song holen
            ArrayList<MusicFiles> playlist = musicService.getCurrentPlaylist();
            int pos = musicService.getPosition();
            if (playlist == null || pos < 0 || pos >= playlist.size()) return;
            String songId = playlist.get(pos).getId();

            new AlertDialog.Builder(this)
                    .setTitle("Zur Playlist hinzufügen")
                    .setItems(names, (dialog, which) -> {
                        manager.addSongToPlaylist(which, songId);
                        Toast.makeText(this, "Song zu \"" + playlists.get(which).name + "\" hinzugefügt!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Abbrechen", null)
                    .show();

        });

        // 5. Initialisiere den SeekBar-Updater-Thread
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (musicService != null && musicService.isPlaying()) {
                    int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                    seekBar.setProgress(mCurrentPosition);
                    duration_played.setText(formattedTime(mCurrentPosition));
                }
                // Lasse den Handler weiterlaufen, um die SeekBar zu aktualisieren
                handler.postDelayed(this, 1000);
            }
        });
        shufflebtn.setImageResource(musicService.getShuffleState() ? R.drawable.ic_shuffle_green : R.drawable.ic_shuffle);
        repeatBtn.setImageResource(musicService.getRepeatState() ? R.drawable.ic_repeat_red : R.drawable.ic_repeat);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        musicService = null;
        if (D) Log.d(TAG, "Esel PlayerActivity: onServiceDisconnected");
    }

    @Override
    public void updatePlayPauseButton() {
        if (musicService != null && playPauseBtn != null) {
            if (musicService.isPlaying()) {
                playPauseBtn.setImageResource(R.mipmap.ic_pause);
            } else {
                playPauseBtn.setImageResource(R.mipmap.ic_play_pressed);
            }
        }
    }

    public void cancelNotification() {
        if (D) Log.d(TAG, "Esel PlayerActivity: cancelNotification");
        finish();
    }

    public void updateShuffleRepeatButtons() {
        if (musicService == null) return;
        runOnUiThread(() -> {
            shufflebtn.setImageResource(musicService.getShuffleState() ? R.drawable.ic_shuffle_green : R.drawable.ic_shuffle);
            repeatBtn.setImageResource(musicService.getRepeatState() ? R.drawable.ic_repeat_red : R.drawable.ic_repeat);
        });
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        String userInput = newText.toLowerCase();

        // Erstelle eine neue, leere Liste für die Suchergebnisse.
        ArrayList<MusicFiles> searchResults = new ArrayList<>();

        if (musicService != null) {
            // Durchsuche die AKTUELLE Playlist des Services
            ArrayList<MusicFiles> currentPlaylist = musicService.getCurrentPlaylist();
            if (currentPlaylist != null) {
                for (MusicFiles song : currentPlaylist) {
                    if (song.getTitle().toLowerCase().contains(userInput)) {
                        searchResults.add(song);
                    }
                }
            }
        }
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (musicService == null) return false;
        int pos = musicService.getPosition();
        ArrayList<MusicFiles> playlist = musicService.getCurrentPlaylist();
        if (playlist == null || pos < 0 || pos >= playlist.size()) return false;

        MusicFiles currentSong = playlist.get(pos);
        int itemId = item.getItemId();

        if (itemId == R.id.menu_open_album) {
            // Album öffnen → AlbumDetails
            Intent intent = new Intent(this, AlbumDetails.class);
            intent.putExtra("albumName", currentSong.getAlbum());
            startActivity(intent);
            return true;

        } else if (itemId == R.id.menu_open_artist) {
            // Interpret öffnen → nach Artist filtern in SongsFragment
            Intent intent = new Intent(this, ArtistActivity.class);
            intent.putExtra("artistName", currentSong.getArtist());
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        // UI einfach neu syncen
        updateUiComponents();
    }

    private void goToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("targetFragment", "SongsFragment");
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        // Slide-Down Animation
        overridePendingTransition(0, R.anim.slide_down);
    }
}
