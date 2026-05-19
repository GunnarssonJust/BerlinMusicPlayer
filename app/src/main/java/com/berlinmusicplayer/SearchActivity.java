package com.berlinmusicplayer;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchActivity extends AppCompatActivity {

    EditText searchInput;
    RecyclerView recyclerView;
    TextView emptyText;
    SearchResultAdapter searchResultAdapter;
    ArrayList<SearchResultItem> results = new ArrayList<>();

    // Debounce: 300ms warten nach letztem Tastendruck
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;

    // Einzelner Hintergrund-Thread für die Suche
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        searchInput = findViewById(R.id.search_input);
        recyclerView = findViewById(R.id.search_recyclerView);
        emptyText = findViewById(R.id.search_empty_text);

        ArrayList<MusicFiles> allSongs = PlaylistManager.getInstance(this).getMusicFiles();
        if(allSongs == null) allSongs = new ArrayList<>();

        searchResultAdapter = new SearchResultAdapter(this, results, allSongs);
        recyclerView.setAdapter(searchResultAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Keyboard sofort öffnen
        searchInput.requestFocus();

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Vorherige ausstehende Suche abbrechen
                if (debounceRunnable != null) {
                    debounceHandler.removeCallbacks(debounceRunnable);
                }

                String query = s.toString().trim();

                if (query.isEmpty()) {
                    results.clear();
                    searchResultAdapter.notifyDataSetChanged();
                    emptyText.setVisibility(View.GONE);
                    return;
                }

                // 300ms warten, dann suchen
                debounceRunnable = () -> performSearch(query);
                debounceHandler.postDelayed(debounceRunnable, 300);
            }
        });
    }

    private void performSearch(String query) {
        executor.execute(() -> {
            // MediaStore SQL-Suche im Hintergrund-Thread
            ArrayList<SearchResultItem> newResults = searchInMediaStore(query);

            // Ergebnisse im UI-Thread anzeigen
            runOnUiThread(() -> {
                results.clear();
                results.addAll(newResults);
                searchResultAdapter.notifyDataSetChanged();
                emptyText.setVisibility(results.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    private ArrayList<SearchResultItem> searchInMediaStore(String query) {
        ArrayList<SearchResultItem> newResults = new ArrayList<>();
        ArrayList<String> addedAlbums = new ArrayList<>();
        ArrayList<String> addedArtists = new ArrayList<>();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.YEAR,
                //MediaStore.Audio.Media.GENRE
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.SIZE
        };

        // SQL LIKE direkt im MediaStore — kein Java-Loop über 34.000 Songs!
        String selection =
                MediaStore.Audio.Media.TITLE + " LIKE ? OR " +
                        MediaStore.Audio.Media.ALBUM + " LIKE ? OR " +
                        MediaStore.Audio.Media.ARTIST + " LIKE ?";

        String[] selectionArgs = new String[]{
                "%" + query + "%",
                "%" + query + "%",
                "%" + query + "%",
        };

        // Max. 100 Ergebnisse — verhindert Überschwemmung bei kurzen Suchwörtern
        String sortOrder = MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC";

        try (Cursor cursor = getContentResolver().query(
                uri, projection, selection, selectionArgs, sortOrder)) {

            if (cursor != null) {
                String lowerQuery = query.toLowerCase();
                int albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                int titleIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int durationIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                int pathIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                int artistIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int idIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int albumIdIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
                int albumYearIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR);
                int dateAddedIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED);
                int sizeIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);

                // Cursor durchlaufen und Ergebnisse hinzufügen

                while (cursor.moveToNext()) {
                    String album    = cursor.getString(albumColumn);
                    String title    = cursor.getString(titleIdx);
                    String duration = cursor.getString(durationIdx);
                    String path     = cursor.getString(pathIdx);
                    String artist   = cursor.getString(artistIdx);
                    String id       = cursor.getString(idIdx);
                    String albumId  = cursor.getString(albumIdIdx);
                    String albumYear = cursor.getString(albumYearIdx);
                    //String albumgenre = cursor.getString(8);
                    long dateAdded = cursor.getLong(dateAddedIdx);
                    long size = cursor.getLong(sizeIdx);

                    MusicFiles song = new MusicFiles(path, title, artist, album, duration, id, albumId,albumYear,dateAdded,size);

                    // Song-Treffer
                    if (title != null && title.toLowerCase().contains(lowerQuery)) {
                        newResults.add(new SearchResultItem(SearchResultItem.TYPE_SONG, song));
                    }

                    // Album-Treffer (jedes Album nur einmal)
                    if (album != null && album.toLowerCase().contains(lowerQuery)
                            && !addedAlbums.contains(albumId)) {
                        addedAlbums.add(albumId);
                        newResults.add(new SearchResultItem(SearchResultItem.TYPE_ALBUM, song));
                    }

                    // Artist-Treffer (jeden Artist nur einmal)
                    if (artist != null && artist.toLowerCase().contains(lowerQuery)
                            && !addedArtists.contains(artist)) {
                        addedArtists.add(artist);
                        newResults.add(new SearchResultItem(SearchResultItem.TYPE_ARTIST, song));
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("SearchActivity", "Fehler bei MediaStore-Suche: " + e.getMessage());
        }

        return newResults;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
        executor.shutdown();
    }
}
