package edu.scu.phantommaze.audio;

import edu.scu.phantommaze.model.Sound;
import edu.scu.phantommaze.model.SoundListener;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Plays the game's sound effects with Java's built-in audio (javax.sound.sampled).
 *
 * <p>Every sound is loaded once at startup into a few {@link Clip}s (a Clip is a sound held
 * in memory, ready to play instantly). Having several copies lets the same sound overlap,
 * e.g. two enemies growling at once. The Doom sounds are mono; they are converted to stereo
 * so they can be panned to the left or right speaker.
 *
 * <p>If the computer has no audio device the game still runs, just silently.
 */
public class SoundPlayer implements SoundListener {

    private static final int COPIES_PER_SOUND = 3;
    private static final float QUIETEST_DB = -30f;

    private final Map<Sound, Clip[]> clips = new EnumMap<>(Sound.class);

    /** Loads every sound. Problems are reported once and the game continues without that sound. */
    public SoundPlayer() {
        for (Sound sound : Sound.values()) {
            try {
                clips.put(sound, load(sound));
            } catch (IOException | UnsupportedAudioFileException | LineUnavailableException
                    | IllegalArgumentException | SecurityException e) {
                System.err.println("Sound disabled: " + sound + " (" + e.getMessage() + ")");
            }
        }
    }

    @Override
    public void play(Sound sound, double volume, double pan) {
        Clip[] copies = clips.get(sound);
        if (copies == null || volume <= 0) {
            return;
        }
        Clip clip = copies[0];
        for (Clip c : copies) { // use a copy that is not already playing
            if (!c.isRunning()) {
                clip = c;
                break;
            }
        }
        clip.stop();
        clip.setFramePosition(0);
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float db = (float) (20 * Math.log10(Math.min(1.0, volume))); // volume to decibels
            gain.setValue(Math.max(Math.max(QUIETEST_DB, gain.getMinimum()), Math.min(gain.getMaximum(), db)));
        }
        if (clip.isControlSupported(FloatControl.Type.PAN)) {
            FloatControl panControl = (FloatControl) clip.getControl(FloatControl.Type.PAN);
            panControl.setValue((float) Math.max(-1.0, Math.min(1.0, pan)));
        }
        clip.start();
    }

    private static Clip[] load(Sound sound)
            throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        String path = "/freedoom/sounds/" + sound.fileName + ".wav";
        byte[] stereo;
        AudioFormat stereoFormat;
        try (InputStream raw = SoundPlayer.class.getResourceAsStream(path)) {
            if (raw == null) {
                throw new IOException("missing " + path);
            }
            try (AudioInputStream in = AudioSystem.getAudioInputStream(new BufferedInputStream(raw))) {
                // Doom sounds are 8-bit mono; convert to 16-bit mono, then copy each sample to both ears.
                AudioFormat source = in.getFormat();
                AudioFormat mono16 = new AudioFormat(source.getSampleRate(), 16, 1, true, false);
                byte[] mono = AudioSystem.getAudioInputStream(mono16, in).readAllBytes();
                stereo = new byte[mono.length * 2];
                for (int i = 0; i + 1 < mono.length; i += 2) {
                    stereo[i * 2] = mono[i];
                    stereo[i * 2 + 1] = mono[i + 1];
                    stereo[i * 2 + 2] = mono[i];
                    stereo[i * 2 + 3] = mono[i + 1];
                }
                stereoFormat = new AudioFormat(source.getSampleRate(), 16, 2, true, false);
            }
        }
        Clip[] copies = new Clip[COPIES_PER_SOUND];
        int opened = 0;
        try {
            while (opened < copies.length) {
                Clip clip = AudioSystem.getClip();
                try {
                    clip.open(stereoFormat, stereo, 0, stereo.length);
                } catch (LineUnavailableException | IllegalArgumentException | SecurityException e) {
                    clip.close();
                    throw e;
                }
                copies[opened++] = clip;
            }
        } catch (LineUnavailableException | IllegalArgumentException | SecurityException e) {
            if (opened == 0) {
                throw e; // not even one copy: the sound is disabled
            }
            // The device ran out of lines: keep the copies that did open (less overlap, same sound).
            System.err.println("Sound " + sound + ": only " + opened + " overlapping copies (" + e.getMessage() + ")");
            return Arrays.copyOf(copies, opened);
        }
        return copies;
    }
}
