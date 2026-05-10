package com.berlinmusicplayer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SoundEffectConstants;

public class LetterIndexTextView extends androidx.appcompat.widget.AppCompatTextView {

    public LetterIndexTextView(Context context) {
        super(context);
    }

    public LetterIndexTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LetterIndexTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean performClick() {
        playSoundEffect(SoundEffectConstants.CLICK);
        return super.performClick();
    }
}
