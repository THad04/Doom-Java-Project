package edu.scu.phantommaze.model;

/**
 * Receives the sounds the game wants to play. The model only says <em>what</em> should be
 * heard; the audio code decides <em>how</em> to play it, so the model never touches speakers.
 */
@FunctionalInterface
public interface SoundListener {

    /**
     * Plays a sound.
     *
     * @param sound  which sound
     * @param volume 0 (silent) to 1 (full volume)
     * @param pan    -1 (left speaker) to 1 (right speaker), 0 = centered
     */
    void play(Sound sound, double volume, double pan);
}
