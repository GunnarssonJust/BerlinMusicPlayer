package com.berlinmusicplayer;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

						
								   
						 
							  

import java.util.ArrayList;

public class ArtistFragment extends Fragment {

    private RecyclerView recyclerView;
    private ArtistAdapter artistAdapter;
    private ArrayList<MusicFiles> artist = new ArrayList<>();
    private LinearLayout letterIndex;
    private TextView popupBubble;
    private Handler bubbleHandler;
    private final android.util.SparseIntArray letterPositions = new android.util.SparseIntArray();
    private final Runnable hideBubbleRunnable = this::hidePopupBubble;

 
																 
    public ArtistFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
																	 
											   
        View view = inflater.inflate(R.layout.fragment_artist, container, false);

        recyclerView = view.findViewById(R.id.recyclerView_artist);
        letterIndex = view.findViewById(R.id.letter_index_artist);
        popupBubble = view.findViewById(R.id.letter_popup_artist);
        bubbleHandler = new Handler(Looper.getMainLooper());

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Daten von MainActivity holen
        if (getActivity() instanceof MainActivity) {
            artist = ((MainActivity) getActivity()).getAllArtist();
            if (artist == null) artist = new ArrayList<>();
        }

        artistAdapter = new ArtistAdapter(getContext(), artist);
        recyclerView.setAdapter(artistAdapter);

        buildLetterIndex();
        setupLetterIndex();

        return view;
    }

    private void buildLetterIndex() {
        letterPositions.clear();
        if (artist == null) return;

        for (int i = 0; i < artist.size(); i++) {
            String name = artist.get(i).getArtist();
            if (name == null || name.isEmpty()) continue;
            char c = Character.toUpperCase(name.charAt(0));
            if (letterPositions.indexOfKey(c) < 0) {
                letterPositions.put(c, i);
            }
        }
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

    private void scrollToLetter(String letter) {
        if (letter == null || letter.isEmpty()) return;
        char c = Character.toUpperCase(letter.charAt(0));
        int position = letterPositions.get(c, -1);
        if (position != -1) {
            recyclerView.scrollToPosition(position);
        }
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

    public void updateAdapterList(ArrayList<MusicFiles> filteredList) {
																		  
        if (artistAdapter != null) {
            artistAdapter.updateList(filteredList);
        }
    }

							
    public void setArtistList(ArrayList<MusicFiles> artist) {
																	  
        if (artist == null) return;

        this.artist = artist;

        if (artistAdapter != null) {
            artistAdapter.updateList(artist);
            buildLetterIndex();
        }
    }
    public void refreshAdapter() {
        if (artistAdapter != null) {
            artistAdapter.notifyDataSetChanged();
        }
    }
}
