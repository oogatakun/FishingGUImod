package com.example.fishinggui.level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FishingProgressTest {
    private static final LevelCurve C = LevelCurve.DEFAULT;

    @Test
    void defaultCurveIsFiftyTimesLevel() {
        assertEquals(50, C.xpToNext(1));
        assertEquals(450, C.xpToNext(9));
        assertEquals(950, C.xpToNext(19));
        assertEquals(0, C.xpToNext(20));
    }

    @Test
    void addXpBelowThresholdKeepsLevel() {
        FishingProgress p = FishingProgress.START.addXp(30, C);
        assertEquals(new FishingProgress(1, 30), p);
    }

    @Test
    void addXpCarriesOverIntoNextLevel() {
        FishingProgress p = FishingProgress.START.addXp(70, C);
        assertEquals(new FishingProgress(2, 20), p);
    }

    @Test
    void addXpCanRaiseSeveralLevelsAtOnce() {
        // 50 (Lv1) + 100 (Lv2) = 150, remaining 10
        FishingProgress p = FishingProgress.START.addXp(160, C);
        assertEquals(new FishingProgress(3, 10), p);
    }

    @Test
    void maxLevelPinsXpToZero() {
        FishingProgress p = new FishingProgress(19, 0).addXp(100000, C);
        assertEquals(new FishingProgress(20, 0), p);
        assertTrue(p.atMax());
        assertEquals(p, p.addXp(500, C));
    }

    @Test
    void nonPositiveAmountChangesNothing() {
        FishingProgress p = new FishingProgress(4, 12);
        assertEquals(p, p.addXp(0, C));
        assertEquals(p, p.addXp(-5, C));
    }

    @Test
    void withLevelClampsAndResetsXp() {
        assertEquals(new FishingProgress(20, 0), new FishingProgress(2, 7).withLevel(99));
        assertEquals(new FishingProgress(1, 0), new FishingProgress(5, 7).withLevel(-3));
        assertEquals(new FishingProgress(8, 0), new FishingProgress(5, 7).withLevel(8));
    }

    @Test
    void withXpReplacesXpAndCarriesOver() {
        assertEquals(new FishingProgress(3, 25), new FishingProgress(3, 90).withXp(25, C));
        // Lv3 needs 150, so 160 carries into Lv4 with 10
        assertEquals(new FishingProgress(4, 10), new FishingProgress(3, 0).withXp(160, C));
    }
}
