package com.berlinmusicplayer;

import android.media.audiofx.Equalizer;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class EqualizerManager {
    private static final String TAG = "EqualizerManager";
    private Equalizer equalizer;
    private static EqualizerManager instance;

    // Speichere EQ-Werte für verschiedene Audio Sessions
    private Map<Integer, short[]> eqPresets = new HashMap<>();

    private EqualizerManager() {}

    public static EqualizerManager getInstance() {
        if (instance == null) {
            instance = new EqualizerManager();
        }
        return instance;
    }

    /**
     * Initialisiere Equalizer für eine Audio Session
     */
    public void initEqualizer(int audioSessionId) {
        try {
            if (equalizer != null) {
                equalizer.release();
            }

            equalizer = new Equalizer(0, audioSessionId);
            equalizer.setEnabled(true);
            Log.d(TAG, "Equalizer initialisiert für Session: " + audioSessionId);

        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Initialisieren des Equalizers", e);
        }
    }

    /**
     * Gib die Anzahl der Frequenzbänder zurück
     */
    public short getNumberOfBands() {
        if (equalizer == null) return 0;
        return equalizer.getNumberOfBands();
    }

    /**
     * Gib die untere Frequenz eines Bands zurück (in mHz)
     */
    public int getBandFrequency(short band) {
        if (equalizer == null) return 0;
        return equalizer.getCenterFreq(band);
    }

    /**
     * Setze den Gain für ein Band
     * @param band Band-Index (0 = tiefste, max = höchste)
     * @param gain Wert in mB (-1200 bis +1200, also -12dB bis +12dB)
     */
    public void setBandGain(short band, short gain) {
        if (equalizer == null) return;
        try {
            equalizer.setBandLevel(band, gain);
            Log.d(TAG, "Band " + band + " auf " + gain + "mB gesetzt");
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Setzen des Gains", e);
        }
    }

    /**
     * Gib den aktuellen Gain eines Bands zurück
     */
    public short getBandGain(short band) {
        if (equalizer == null) return 0;
        try {
            return equalizer.getBandLevel(band);
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Lesen des Gains", e);
            return 0;
        }
    }

    /**
     * Gib min und max Gain-Wert zurück
     */
    public short[] getBandLevelRange() {
        if (equalizer == null) return new short[]{-1200, 1200};
        return equalizer.getBandLevelRange();
    }

    /**
     * Preset: Bass Boost
     */
    public void applyBassBoost() {
        if (equalizer == null) return;
        short numBands = getNumberOfBands();
        // Die unteren 2-3 Bänder hochfahren
        setBandGain((short) 0, (short) 800);   // Tiefbass
        if (numBands > 1) setBandGain((short) 1, (short) 600);
    }

    /**
     * Preset: Treble Boost
     */
    public void applyTrebleBoost() {
        if (equalizer == null) return;
        short numBands = getNumberOfBands();
        // Die oberen 2-3 Bänder hochfahren
        setBandGain((short) (numBands - 1), (short) 800);
        if (numBands > 1) setBandGain((short) (numBands - 2), (short) 600);
    }

    /**
     * Preset: Flat (alle Bänder auf 0)
     */
    public void applyFlat() {
        if (equalizer == null) return;
        short numBands = getNumberOfBands();
        for (short i = 0; i < numBands; i++) {
            setBandGain(i, (short) 0);
        }
    }

    /**
     * Speichere aktuellen EQ-Zustand
     */
    public void savePreset(String presetName) {
        if (equalizer == null) return;
        short numBands = getNumberOfBands();
        short[] gains = new short[numBands];
        for (short i = 0; i < numBands; i++) {
            gains[i] = getBandGain(i);
        }
        eqPresets.put(presetName.hashCode(), gains);
    }

    /**
     * Lade gespeicherten EQ-Zustand
     */
    public void loadPreset(String presetName) {
        short[] gains = eqPresets.get(presetName.hashCode());
        if (gains != null) {
            for (short i = 0; i < gains.length; i++) {
                setBandGain(i, gains[i]);
            }
        }
    }

    /**
     * Release Equalizer
     */
    public void release() {
        if (equalizer != null) {
            equalizer.release();
            equalizer = null;
            Log.d(TAG, "Equalizer released");
        }
    }

    /**
     * Prüfe ob Equalizer aktiv ist
     */
    public boolean isEnabled() {
        return equalizer != null && equalizer.getEnabled();
    }
}
