package com.example.fishinggui.fight;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BaitEffectTest {
    private static final FishProfile BASE = new FishProfile(100f, 70, 1.0f, 1.0f);

    @Test
    void noneAndWormLeaveTheProfileUnchanged() {
        assertEquals(BASE, BaitEffect.apply(BaitType.NONE, BASE));
        assertEquals(BASE, BaitEffect.apply(BaitType.WORM, BASE));
        assertEquals(BASE, BaitEffect.apply(BaitType.GLOW_BUG, BASE));
    }

    @Test
    void sturdyPasteWeakensDashesOnly() {
        FishProfile p = BaitEffect.apply(BaitType.STURDY_PASTE, BASE);
        assertEquals(0.6f, p.dashPower(), 1e-6f);
        assertEquals(BASE.stamina(), p.stamina());
        assertEquals(BASE.dashInterval(), p.dashInterval());
        assertEquals(BASE.tensionGain(), p.tensionGain());
    }

    @Test
    void onlyGlowBugAddsLuck() {
        assertEquals(0f, BaitEffect.luckBonus(BaitType.NONE));
        assertEquals(0f, BaitEffect.luckBonus(BaitType.WORM));
        assertEquals(2f, BaitEffect.luckBonus(BaitType.GLOW_BUG));
        assertEquals(0f, BaitEffect.luckBonus(BaitType.STURDY_PASTE));
    }
}
