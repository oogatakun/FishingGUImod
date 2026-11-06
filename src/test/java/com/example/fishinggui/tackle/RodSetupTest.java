package com.example.fishinggui.tackle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.fishinggui.fight.FishProfile;
import org.junit.jupiter.api.Test;

class RodSetupTest {
    @Test
    void needsRodLineAndHook() {
        RodSetup s = new RodSetup();
        assertFalse(s.complete());
        s = s.with(Part.ROD_BLANK, 1).with(Part.LINE, 1);
        assertFalse(s.complete());
        s = s.with(Part.HOOK, 1);
        assertTrue(s.complete());
    }

    @Test
    void reelIsOptional() {
        RodSetup s = new RodSetup().with(Part.ROD_BLANK, 2).with(Part.LINE, 1).with(Part.HOOK, 1);
        assertTrue(s.complete());
        assertEquals(1.0, s.reelSpeed());
        assertEquals(10, s.levelCap());
    }

    @Test
    void higherRanksAreBetterEverywhere() {
        for (int r = 1; r < Part.MAX_RANK; r++) {
            RodSetup lo = new RodSetup().with(Part.ROD_BLANK, r).with(Part.REEL, r).with(Part.LINE, r)
                    .with(Part.HOOK, r).with(Part.FLOAT, r).with(Part.LEADER, r);
            RodSetup hi = new RodSetup().with(Part.ROD_BLANK, r + 1).with(Part.REEL, r + 1).with(Part.LINE, r + 1)
                    .with(Part.HOOK, r + 1).with(Part.FLOAT, r + 1).with(Part.LEADER, r + 1);
            assertTrue(hi.levelCap() > lo.levelCap());
            assertTrue(hi.durability() > lo.durability());
            assertTrue(hi.reelSpeed() > lo.reelSpeed());
            assertTrue(hi.tensionScale() < lo.tensionScale());
            assertTrue(hi.hookLuck() > lo.hookLuck());
            assertTrue(hi.biteCut() > lo.biteCut());
            assertTrue(hi.dashScale() < lo.dashScale());
        }
    }

    @Test
    void topRodReachesTheHighestFishLevel() {
        assertEquals(20, new RodSetup().with(Part.ROD_BLANK, 4).levelCap());
    }

    @Test
    void applyScalesTheProfile() {
        RodSetup s = new RodSetup().with(Part.REEL, 4).with(Part.LINE, 4);
        FishProfile p = s.apply(new FishProfile(140f, 70, 1.0f, 1.0f));
        assertEquals(100f, p.stamina(), 0.01f);
        assertEquals(0.76f, p.tensionGain(), 0.001f);
        assertEquals(70, p.dashInterval());
    }

    @Test
    void ranksAreClamped() {
        RodSetup s = new RodSetup(new int[] {9, -3});
        assertEquals(Part.MAX_RANK, s.rank(Part.ROD_BLANK));
        assertEquals(0, s.rank(Part.REEL));
    }
}
