package com.berlinmusicplayer;

public interface ActionPlaying {
    void nextBtnClicked();
    void prevBtnClicked();

    void updatePlayerView();
    void updatePlayPauseButton();
    void cancelNotification();
    void updateShuffleRepeatButtons();
}
