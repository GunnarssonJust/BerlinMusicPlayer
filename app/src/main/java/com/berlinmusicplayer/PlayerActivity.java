package com.berlinmusicplayer;

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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Outline;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Random;

public class PlayerActivity extends AppCompatActivity implements ActionPlaying, ServiceConnection, SearchView.OnQueryTextListener {

    private final boolean D = true;
    private static final String TAG = "PlayerActivity";
    TextView song_name, artist, duration_played, duration_total;
    ImageView cover_art, nextBtn, prevBtn, shufflebtn, repeatBtn, playPauseBtn,playListButton;
    private FrameLayout vinylContainer;
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
    private Animation vinylRotation;
    private boolean isVinylDesign = false;
    private float vinylRotationOffset = 0f;
    private ImageView toneArm;
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
        vinylContainer = findViewById(R.id.vinyl_container);
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

        loadPlayerDesign();
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
        if (musicService != null && musicService.isPlaying()) {
            handler.post(updateSeekbar);
        }
    }

    @Override
    protected void onPause() {
        //App geht in Hintergrund, neue App, Homebutton geklickt etc.
        super.onPause();
        //Seekbar Updates stoppen wenn nicht sichtbar.
        if (handler != null) {
            handler.removeCallbacks(updateSeekbar);
        }

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
            if (isVinylDesign) {
                if (musicService.isPlaying()) {
                    startVinylRotation();
                } else {
                    stopVinylRotation();
                }
            }
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
        toneArm = findViewById(R.id.vinyl_tonearm);

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
        if (isVinylDesign) {
            if (musicService != null && musicService.isPlaying()) {
                startVinylRotation();
            } else {
                stopVinylRotation();
            }
        }
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
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
            bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
            if (bitmap== null){
                bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_play_pressed);
            }
        } else {
            bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_play_pressed);
        }
        ImageAnimation(this, cover_art, bitmap);
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
        handler.post(updateSeekbar);

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
        handler.post(updateSeekbar);
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
        }else if(itemId == R.id.menu_share) {}//TODO
        else if(itemId == R.id.player_design){
            showPlayerDesignDialog();
        } else {}//TODO
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

    private final Runnable updateSeekbar = new Runnable() {
        @Override
        public void run() {
            if (musicService != null && musicService.isPlaying()) {
                int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                seekBar.setProgress(mCurrentPosition);
                duration_played.setText(formattedTime(mCurrentPosition));
            }
            handler.postDelayed(this, 1000);
        }
    };

    private void loadPlayerDesign() {
        int design = getSharedPreferences("player_prefs", MODE_PRIVATE)
                .getInt("player_design", 0);
        applyDesign(design);
    }
    // Design anwenden:
    private void applyDesign(int design) {
        switch (design) {
            case 0:
                applyClassicDesign();
                break;
            case 1:
                applyVinylDesign();
                break;
        }
    }

    // Classic Design (so wie jetzt):
    private void applyClassicDesign() {
        isVinylDesign = false;
        toneArm.setVisibility(View.INVISIBLE);
        toneArm.clearAnimation();
        stopVinylRotation();
        cover_art.setClipToOutline(false);
        ViewGroup.LayoutParams params = cover_art.getLayoutParams();
        ViewGroup.LayoutParams containerParams = vinylContainer.getLayoutParams();
        containerParams.width = dpToPx(320);
        containerParams.height = dpToPx(320);
        vinylContainer.setLayoutParams(containerParams);
        params.width = dpToPx(320);
        params.height = dpToPx(320);
        cover_art.setLayoutParams(params);
        vinylContainer.setVisibility(View.VISIBLE);
        findViewById(R.id.vinyl_ring).setVisibility(View.INVISIBLE);
        findViewById(R.id.vinyl_hole).setVisibility(View.INVISIBLE);
    }

    // Vinyl Design:
    private void applyVinylDesign() {
        isVinylDesign = true;

        // ← Container auf 350x350 in der klassischen Ansicht zurücksetzen
        ViewGroup.LayoutParams containerParams = vinylContainer.getLayoutParams();
        containerParams.width = dpToPx(280);
        containerParams.height = dpToPx(280);
        vinylContainer.setLayoutParams(containerParams);

        // Cover klein und rund!
        ViewGroup.LayoutParams params = cover_art.getLayoutParams();
        params.width = dpToPx(152);
        params.height = dpToPx(152);
        cover_art.setLayoutParams(params);

        cover_art.setClipToOutline(true);
        cover_art.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });

        // vinyl_ring und vinyl_hole zeigen
        findViewById(R.id.vinyl_ring).setVisibility(View.VISIBLE);
        findViewById(R.id.vinyl_hole).setVisibility(View.VISIBLE);
        toneArm.setVisibility(View.VISIBLE);
        toneArm.setRotation(-25f);
        vinylRotation = AnimationUtils.loadAnimation(PlayerActivity.this, R.anim.rotate_vinyl);
        if (musicService != null && musicService.isPlaying()) {
            startVinylRotation();
        }
    }

    // Rotation starten:
    private void startVinylRotation() {
        if (!isVinylDesign || vinylRotation == null || vinylContainer == null) return;
        vinylContainer.startAnimation(vinylRotation);
        if (!isVinylDesign || vinylRotation == null || vinylContainer == null) return;
        vinylContainer.startAnimation(vinylRotation);
        moveToneArmToPlay();  // ← NEU: Arm auf Platte!
    }

    // Rotation stoppen (Position merken):
    private void stopVinylRotation() {
        if (vinylContainer != null) {
            // Aktuelle Rotation merken für nahtlosen Neustart!
            vinylRotationOffset = vinylContainer.getRotation();
            vinylContainer.clearAnimation();
            }
            moveToneArmToPause();  // ← NEU: Arm zurückziehen!
        }

    // Design-Auswahl Dialog — z.B. im Options-Menü aufrufen:
    private void showPlayerDesignDialog() {
        String[] designs = {"🎨 Classic", "💿 Vinyl"};
        int current = getSharedPreferences("player_prefs", MODE_PRIVATE)
                .getInt("player_design", 0);

        new AlertDialog.Builder(this)
                .setTitle("Player Design")
                .setSingleChoiceItems(designs, current, (dialog, which) -> {
                    getSharedPreferences("player_prefs", MODE_PRIVATE)
                            .edit()
                            .putInt("player_design", which)
                            .apply();
                    applyDesign(which);
                    dialog.dismiss();
                })
                .show();
    }
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
    // 5. Tonarm-Animationen:
    private void moveToneArmToPlay() {
        if (!isVinylDesign || toneArm == null) return;
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.tonearm_play);
        anim.setFillAfter(true);
        toneArm.startAnimation(anim);
    }

    private void moveToneArmToPause() {
        if (!isVinylDesign || toneArm == null) return;
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.tonearm_pause);
        anim.setFillAfter(true);
        toneArm.startAnimation(anim);
    }
}
