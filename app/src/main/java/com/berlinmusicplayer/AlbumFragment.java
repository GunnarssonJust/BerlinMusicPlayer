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

public class AlbumFragment extends Fragment {

    RecyclerView recyclerView;
    AlbumAdapter albumAdapter;
    private ArrayList<MusicFiles> albumList = new ArrayList<>();
    private LinearLayout letterIndex;
    private TextView popupBubble;
    private Handler bubbleHandler;
    private final android.util.SparseIntArray letterPositions = new android.util.SparseIntArray();
    private final Runnable hideBubbleRunnable = this::hidePopupBubble;

    public AlbumFragment() {
		//required empty public constructor
	}
											
	 

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
											   
        View view = inflater.inflate(R.layout.fragment_album, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        letterIndex = view.findViewById(R.id.letter_index_album);
        popupBubble = view.findViewById(R.id.letter_popup_album);
        bubbleHandler = new Handler(Looper.getMainLooper());

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Daten von MainActivity holen
        if (getActivity() instanceof MainActivity) {
            albumList = ((MainActivity) getActivity()).getAllAlbum();
            if (albumList == null) albumList = new ArrayList<>();
        }
										   

        albumAdapter = new AlbumAdapter(getContext(), albumList);
        recyclerView.setAdapter(albumAdapter);

        buildLetterIndex();
        setupLetterIndex();

        return view;
    }

    private void buildLetterIndex() {
        letterPositions.clear();
        if (albumList == null) return;

        for (int i = 0; i < albumList.size(); i++) {
            String album = albumList.get(i).getAlbum();
            if (album == null || album.isEmpty()) continue;
            char c = Character.toUpperCase(album.charAt(0));
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
        if (albumAdapter != null) {
            albumAdapter.updateList(filteredList);
        }
    }

    public void setAlbumList(ArrayList<MusicFiles> albumList) {
        if (albumList == null) return;
        this.albumList = albumList;
        if (albumAdapter != null) {
            albumAdapter.updateList(albumList);
            buildLetterIndex();
        }
    }
    public void refreshAdapter() {
        if (albumAdapter != null) {
            albumAdapter.notifyDataSetChanged();
        }
    }
}
