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
    void higherRanksAreBetterEverywhereWhileTheRodCanCarryThem() {
        for (int r = 1; r < 3; r++) {
            RodSetup lo = new RodSetup().with(Part.ROD_BLANK, 4).with(Part.REEL, r).with(Part.LINE, r)
                    .with(Part.HOOK, r).with(Part.FLOAT, r).with(Part.LEADER, r);
            RodSetup hi = new RodSetup().with(Part.ROD_BLANK, 4).with(Part.REEL, r + 1).with(Part.LINE, r + 1)
                    .with(Part.HOOK, r + 1).with(Part.FLOAT, r + 1).with(Part.LEADER, r + 1);
            assertEquals(0, hi.overweight());
            assertTrue(hi.reelSpeed() > lo.reelSpeed());
            assertTrue(hi.tensionScale() < lo.tensionScale());
            assertTrue(hi.hookLuck() > lo.hookLuck());
            assertTrue(hi.biteCut() > lo.biteCut());
            assertTrue(hi.dashScale() < lo.dashScale());
        }
    }

    private static RodSetup all(int rank) {
        RodSetup s = new RodSetup();
        for (Part p : Part.values()) {
            s = s.with(p, rank);
        }
        return s;
    }

    @Test
    void everythingInNetheriteIsOverweight() {
        RodSetup s = all(4);
        assertEquals(48, s.weight());
        assertEquals(36, s.capacity());
        assertEquals(12, s.overweight());
        assertEquals(1.40 * 0.70, s.reelSpeed(), 1e-9);
        assertEquals(0.76 * 1.12, s.tensionScale(), 1e-9);
        assertEquals(0.20 - 0.06, s.biteCut(), 1e-9);
    }

    @Test
    void lighterPartsOnANetheriteRodBeatTheFullSet() {
        RodSetup mixed = all(4).with(Part.LEADER, 1).with(Part.FLOAT, 1).with(Part.LURE, 1);
        assertEquals(0, mixed.overweight());
        assertTrue(mixed.reelSpeed() > all(4).reelSpeed());
        assertTrue(mixed.tensionScale() < all(4).tensionScale());
    }

    @Test
    void weightIsOnlyPenalisedOverTheCapacity() {
        assertEquals(0, all(1).overweight());
        assertEquals(1.10, all(1).reelSpeed(), 1e-9);
        assertEquals(0, new RodSetup().with(Part.REEL, 4).with(Part.LINE, 4).overweight());
    }

    @Test
    void noBuildIsBestEverywhere() {
        double[] best = new double[8];
        java.util.List<double[]> all = new java.util.ArrayList<>();
        int[] opt = {0, 1, 2, 3, 4};
        for (int rod = 1; rod <= 4; rod++) for (int line = 1; line <= 4; line++) for (int hook = 1; hook <= 4; hook++)
            for (int reel : opt) for (int fl : opt) for (int ld : opt) for (int lure : opt) {
                RodSetup s = new RodSetup().with(Part.ROD_BLANK, rod).with(Part.LINE, line).with(Part.HOOK, hook)
                        .with(Part.REEL, reel).with(Part.FLOAT, fl).with(Part.LEADER, ld).with(Part.LURE, lure);
                double[] v = {s.levelCap(), s.durability(), s.reelSpeed(), 1.0 / s.tensionScale(), s.hookLuck(),
                        s.biteCut(), 1.0 / s.dashScale(), lure};
                all.add(v);
                for (int i = 0; i < v.length; i++) {
                    best[i] = Math.max(best[i], v[i]);
                }
            }
        for (double[] v : all) {
            boolean bestEverywhere = true;
            for (int i = 0; i < v.length; i++) {
                bestEverywhere &= v[i] >= best[i] - 1e-9;
            }
            assertFalse(bestEverywhere);
        }
    }

    @Test
    void aStyleTradesItsPowerForWeight() {
        RodSetup plain = new RodSetup().with(Part.REEL, 2);
        RodSetup heavy = new RodSetup().with(Part.REEL, 2, Style.BASS_BAIT);
        RodSetup light = new RodSetup().with(Part.REEL, 2, Style.BASS_SPINNING);
        assertTrue(heavy.reelSpeed() > plain.reelSpeed());
        assertTrue(light.reelSpeed() < plain.reelSpeed());
        assertEquals(4, RodSetup.weightOf(2, null));
        assertEquals(6, RodSetup.weightOf(2, Style.BASS_BAIT));
        assertEquals(3, RodSetup.weightOf(2, Style.BASS_SPINNING));
        assertEquals(1.0 + 0.2 * 1.08, heavy.reelSpeed(), 1e-9);
    }

    @Test
    void aStyleOfAnotherRankIsIgnored() {
        assertEquals(null, new RodSetup().with(Part.REEL, 1, Style.BIG_GAME).style(Part.REEL));
        assertEquals(Style.BIG_GAME, new RodSetup().with(Part.REEL, 4, Style.BIG_GAME).style(Part.REEL));
    }

    @Test
    void rodStylesChangeDurabilityAndCapacity() {
        RodSetup big = new RodSetup().with(Part.ROD_BLANK, 4, Style.BIG_GAME);
        assertEquals(2600, big.durability());
        assertEquals(40, big.capacity());
        RodSetup tele = new RodSetup().with(Part.ROD_BLANK, 1, Style.TELESCOPIC);
        assertEquals(90, tele.durability());
        assertEquals(20, tele.capacity());
        assertEquals(5, tele.levelCap());
    }

    @Test
    void lureStylesChangeTheChance() {
        RodSetup s = new RodSetup().with(Part.LURE, 4, Style.BIG_GAME).withLureSpecies("fishinggui:aji");
        assertEquals(0.12, s.lureChance(), 1e-9);
        assertTrue(s.lureCalls(0.11, 10));
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
    void lureCallsItsSpeciesOnlyOnALowRollAndWithinItsLevels() {
        RodSetup s = new RodSetup().with(Part.LURE, 2).withLureSpecies("fishinggui:aji");
        assertTrue(s.lureCalls(0.05, 10));
        assertFalse(s.lureCalls(0.5, 10));
        assertFalse(s.lureCalls(0.05, 11));
        assertFalse(s.with(Part.LURE, 0).lureCalls(0.05, 1));
        assertFalse(new RodSetup().with(Part.LURE, 2).lureCalls(0.05, 1));
    }

    @Test
    void withKeepsTheLureSpecies() {
        RodSetup s = new RodSetup().withLureSpecies("fishinggui:aji").with(Part.HOOK, 2);
        assertEquals("fishinggui:aji", s.lureSpecies());
    }

    @Test
    void aBetterLineLastsLonger() {
        for (int r = 1; r < Part.MAX_RANK; r++) {
            assertTrue(RodSetup.lineLife(r + 1) > RodSetup.lineLife(r));
        }
        assertEquals(0, RodSetup.lineLife(0));
        assertTrue(new RodSetup().with(Part.LINE, 1).hasParts());
        assertFalse(new RodSetup().hasParts());
    }

    @Test
    void ranksAreClamped() {
        RodSetup s = new RodSetup(new int[] {9, -3});
        assertEquals(Part.MAX_RANK, s.rank(Part.ROD_BLANK));
        assertEquals(0, s.rank(Part.REEL));
    }
}
