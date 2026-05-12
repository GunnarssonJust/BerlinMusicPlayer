package com.berlinmusicplayer;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.ArrayList;

public class SongsFragment extends Fragment {

    private FastScrollRecyclerView recyclerView;
    private ImageButton shuffleButton, playListButton;
    private MusicAdapter musicAdapter;
    private LinearLayout letterIndex;
    private TextView popupBubble;
    private Handler bubbleHandler;
    private ArrayList<MusicFiles> musicFiles = new ArrayList<>();

    private final Runnable hideBubbleRunnable = this::hidePopupBubble;
    private final android.util.SparseIntArray letterPositions = new android.util.SparseIntArray();
    public SongsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_songs, container, false);

        // Views
        recyclerView = view.findViewById(R.id.recyclerView);
        shuffleButton = view.findViewById(R.id.shuffle_all_songs);

        letterIndex = view.findViewById(R.id.letter_index);
        popupBubble = view.findViewById(R.id.letter_popup);
        bubbleHandler = new Handler(Looper.getMainLooper());

        if (getActivity() instanceof MainActivity) {
            musicFiles = (((MainActivity) getActivity()).getAllAudio());
        }
        // RecyclerView
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        musicAdapter = new MusicAdapter(getContext(), musicFiles);
        recyclerView.setAdapter(musicAdapter);
        buildLetterIndex();


        // Letter-Index + Popup
        setupLetterIndex();

        // Shuffle-Button
        shuffleButton.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity main = (MainActivity) getActivity();
                if (main.musicService != null) main.musicService.shuffleAll();
            }
        });

        return view;
    }

    public void refreshList() {
        if (musicAdapter != null) {
            musicAdapter.updateList(musicFiles,true);
            musicAdapter.notifyDataSetChanged();
            buildLetterIndex();
        }
    }

    public void updateAdapterList(ArrayList<MusicFiles> filteredList, boolean isSearchResult) {
        if (musicAdapter != null) {
            musicAdapter.updateList(filteredList, isSearchResult);
        }
    }
    public void updateMusicList(ArrayList<MusicFiles> newList, boolean isSearchResult) {
        // ...
        if (musicAdapter != null) {
            musicAdapter.updateList(newList, isSearchResult); // Gib den Parameter weiter
        }
    }

    private void buildLetterIndex() {
        letterPositions.clear();
        if (musicFiles == null) return;

        for (int i = 0; i < musicFiles.size(); i++) {
            String title = musicFiles.get(i).getTitle();
            if (title == null || title.isEmpty()) continue;

            char c = Character.toUpperCase(title.charAt(0));
            // Nur speichern wenn dieser Buchstabe noch nicht erfasst ist
            if (letterPositions.indexOfKey(c) < 0) {
                letterPositions.put(c, i);
            }
        }
    }
    // scrollToLetter() ersetzen:
    private void scrollToLetter(String letter) {
        if (letter == null || letter.isEmpty()) return;
        char c = Character.toUpperCase(letter.charAt(0));
        int position = letterPositions.get(c, -1);
        if (position != -1) {
            recyclerView.scrollToPosition(position);
        }
    }
	// 👉 zentrale Methode zum Setzen der Daten
    public void setMusicList(ArrayList<MusicFiles> newList) {
        if (newList == null) return;

        this.musicFiles = newList;

        if (musicAdapter != null) {
            musicAdapter.updateList(newList, false);
        }

        buildLetterIndex();
    }
    @SuppressLint("ClickableViewAccessibility")
    private void setupLetterIndex() {
        // ← Buchstaben programmatisch hinzufügen!
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        for (char c : alphabet.toCharArray()) {
            TextView letter = new TextView(requireContext());
            letter.setText(String.valueOf(c));
            letter.setTextSize(10);
            letter.setTextColor(Color.WHITE);
            letter.setPadding(2, 1, 2, 1);
            letter.setGravity(Gravity.CENTER);
            letter.setTypeface(null, android.graphics.Typeface.BOLD);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0, 1f);  // ← Gleichmäßig verteilen
            letter.setLayoutParams(params);
            letterIndex.addView(letter);
        }

        // Touch auf dem gesamten LinearLayout
        letterIndex.setOnTouchListener((v, event) -> {
            int y = (int) event.getY();
            int height = letterIndex.getHeight();
            int index = Math.max(0, Math.min((y * 26) / height, 25));
            String letter = String.valueOf((char) ('A' + index));

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    scrollToLetter(letter);
                    showPopupBubble(letter);
                    break;
            }
            return true;
        });
    }

    private void showPopupBubble(String letter) {
        if (popupBubble == null) return;

        popupBubble.setText(letter);
        popupBubble.setAlpha(1f);
        popupBubble.setVisibility(View.VISIBLE);

        bubbleHandler.removeCallbacks(hideBubbleRunnable);
        bubbleHandler.postDelayed(hideBubbleRunnable, 600);
    }

    private void hidePopupBubble() {
        if (popupBubble == null) return;

        popupBubble.animate()
                .alpha(0f)
                .setDuration(150)
                .withEndAction(() -> popupBubble.setVisibility(View.GONE))
                .start();
    }
}
