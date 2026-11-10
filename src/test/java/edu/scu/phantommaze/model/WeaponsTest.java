package edu.scu.phantommaze.model;

import edu.scu.phantommaze.Config;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WeaponsTest {
    @Test
    void pistolClipReloadsWithoutReserveAmmo() {
        Weapons weapons = new Weapons();
        for (int clip = 0; clip < 2; clip++) {
            for (int shot = 0; shot < Config.PISTOL_CLIP_SIZE; shot++) {
                assertTrue(weapons.tryFire());
                weapons.update(Config.PISTOL_FIRE_DELAY);
            }
            assertEquals(0, weapons.getAmmo());
            assertFalse(weapons.tryFire());
            assertTrue(weapons.startReload());
            weapons.update(Config.PISTOL_RELOAD_TIME / 2);
            assertFalse(weapons.tryFire());
            assertTrue(weapons.isReloading());
            weapons.update(Config.PISTOL_RELOAD_TIME / 2);
            assertFalse(weapons.isReloading());
            assertEquals(Config.PISTOL_CLIP_SIZE, weapons.getAmmo());
        }
    }

    @Test
    void shotgunPickupCancelsReloadWithoutRefillingPistol() {
        Weapons weapons = new Weapons();
        assertTrue(weapons.tryFire());
        weapons.update(Config.PISTOL_FIRE_DELAY);
        assertTrue(weapons.startReload());
        weapons.update(0.25);

        weapons.equipShotgun();

        assertFalse(weapons.isReloading());
        assertEquals(Config.SHOTGUN_SHELLS, weapons.getAmmo());
        weapons.update(Config.PISTOL_RELOAD_TIME);
        weapons.equipPistol();
        assertEquals(Config.PISTOL_CLIP_SIZE - 1, weapons.getAmmo());
        assertTrue(weapons.tryFire());
    }

    @Test
    void switchingGunsDoesNotBypassShotCooldown() {
        Weapons weapons = new Weapons();
        weapons.equipShotgun();
        assertTrue(weapons.tryFire());

        weapons.equipPistol();
        assertFalse(weapons.tryFire());
        weapons.update(Config.SHOTGUN_FIRE_DELAY);
        assertTrue(weapons.tryFire());
        weapons.equipShotgun();
        assertFalse(weapons.tryFire());
        weapons.update(Config.PISTOL_FIRE_DELAY);
        assertTrue(weapons.tryFire());
    }

    @Test
    void shotgunHasFiftyShellsAndResetRestoresPistol() {
        Weapons weapons = new Weapons();
        weapons.equipShotgun();
        for (int shell = 0; shell < Config.SHOTGUN_SHELLS; shell++) {
            assertTrue(weapons.tryFire());
            weapons.update(Config.SHOTGUN_FIRE_DELAY);
        }
        assertEquals(0, weapons.getAmmo());
        assertFalse(weapons.tryFire());

        weapons.reset();

        assertEquals(Weapon.PISTOL, weapons.getCurrent());
        assertEquals(Config.PISTOL_CLIP_SIZE, weapons.getAmmo());
        assertFalse(weapons.isReloading());
        assertTrue(weapons.tryFire());
    }
}
