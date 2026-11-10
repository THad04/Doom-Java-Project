package edu.scu.phantommaze.model;

/**
 * Every sound effect the game can ask for. Each one is a Freedoom WAV file in
 * {@code src/main/resources/freedoom/sounds/}.
 */
public enum Sound {
    /** Pistol shot. */
    PISTOL("dspistol"),
    /** Shotgun blast. */
    SHOTGUN("dsshotgn"),
    /** Pistol reload. */
    RELOAD("dsdbload"),
    /** Shotgun power-up picked up. */
    POWER_UP("dsgetpow"),
    /** Power-up wore off and the pistol comes back. */
    WEAPON_UP("dswpnup"),
    /** Player hit by an enemy. */
    PLAYER_PAIN("dsplpain"),
    /** Player hit for the last time. */
    PLAYER_DEATH("dspldeth"),
    /** Cacodemon growl. */
    CACODEMON_SIGHT("dscacsit"),
    /** Cacodemon death. */
    CACODEMON_DEATH("dscacdth"),
    /** Demon growl. */
    DEMON_SIGHT("dssgtsit"),
    /** Demon death. */
    DEMON_DEATH("dssgtdth"),
    /** Lost soul screech. */
    LOST_SOUL_SIGHT("dssklatk"),
    /** Lost soul death (it bursts into flames). */
    LOST_SOUL_DEATH("dsfirxpl"),
    /** Imp growl. */
    IMP_SIGHT("dsbgsit1"),
    /** Imp death. */
    IMP_DEATH("dsbgdth1"),
    /** Pain grunt for the cacodemon, demon and lost soul. */
    DEMON_PAIN("dsdmpain"),
    /** Pain grunt for the imp. */
    IMP_PAIN("dspopain"),
    /** BFG charging up and firing. */
    BFG("dsbfg"),
    /** BFG blast hitting enemies. */
    BFG_EXPLOSION("dsrxplod"),
    /** Rampage mode starting: a burst of flame. */
    RAMPAGE("dsflamst");

    /** File name without the {@code .wav} extension. */
    public final String fileName;

    Sound(String fileName) {
        this.fileName = fileName;
    }
}
