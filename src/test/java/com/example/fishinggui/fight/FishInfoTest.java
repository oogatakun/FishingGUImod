package com.example.fishinggui.fight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class FishInfoTest {
    private static final FishInfo INFO = new FishInfo(Rarity.RARE, 1.0f, 3.0f, 40);
    private static final FishProfile BASE = new FishProfile(100f, 70, 1.0f, 1.0f);

    @Test
    void rolledWeightStaysInRangeAndHasOneDecimal() {
        Random r = new Random(7);
        for (int i = 0; i < 500; i++) {
            float w = INFO.rollWeight(r);
            assertTrue(w >= 1.0f && w <= 3.0f, "weight " + w);
            assertEquals(Math.round(w * 10f) / 10f, w, 1e-6f);
        }
    }

    @Test
    void fixedRangeAlwaysRollsThatWeight() {
        FishInfo fixed = new FishInfo(Rarity.COMMON, 0.5f, 0.5f, 1);
        assertEquals(0.5f, fixed.rollWeight(new Random(1)), 1e-6f);
    }

    @Test
    void scaleForWeightMapsRangeToPointEightThroughOnePointThree() {
        assertEquals(0.8f, INFO.scaleForWeight(BASE, 1.0f).tensionGain(), 1e-6f);
        assertEquals(1.3f, INFO.scaleForWeight(BASE, 3.0f).tensionGain(), 1e-6f);
        assertEquals(1.05f, INFO.scaleForWeight(BASE, 2.0f).tensionGain(), 1e-6f);
    }

    @Test
    void scaleForWeightUsesMiddleWhenRangeIsFixed() {
        FishInfo fixed = new FishInfo(Rarity.COMMON, 0.5f, 0.5f, 1);
        assertEquals(1.05f, fixed.scaleForWeight(BASE, 0.5f).tensionGain(), 1e-6f);
    }

    @Test
    void xpForWeightMapsRangeToPointEightThroughOnePointThree() {
        FishInfo info = new FishInfo(Rarity.COMMON, 1.0f, 3.0f, 100);
        assertEquals(80, info.xpForWeight(1.0f));
        assertEquals(130, info.xpForWeight(3.0f));
        assertEquals(105, new FishInfo(Rarity.COMMON, 0.5f, 0.5f, 100).xpForWeight(0.5f));
    }

    @Test
    void xpForWeightNeverDropsBelowOne() {
        assertEquals(1, new FishInfo(Rarity.COMMON, 1.0f, 3.0f, 0).xpForWeight(1.0f));
    }

    @Test
    void scaleForWeightOnlyTouchesTensionGain() {
        FishProfile p = INFO.scaleForWeight(BASE, 3.0f);
        assertEquals(BASE.stamina(), p.stamina());
        assertEquals(BASE.dashInterval(), p.dashInterval());
        assertEquals(BASE.dashPower(), p.dashPower());
    }

    @Test
    void rarityParsingAndKeys() {
        assertEquals(Rarity.EPIC, Rarity.fromName("Epic"));
        assertEquals("gui.fishinggui.rarity.uncommon", Rarity.UNCOMMON.langKey());
        assertThrows(IllegalArgumentException.class, () -> Rarity.fromName("mythic"));
    }
}
