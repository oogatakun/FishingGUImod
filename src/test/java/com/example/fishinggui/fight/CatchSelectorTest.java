package com.example.fishinggui.fight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.fishinggui.fight.CatchSelector.Entry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

class CatchSelectorTest {
    private static final int N = 40000;

    private static List<Entry> oneFishPerLevel(int maxLevel) {
        List<Entry> pool = new ArrayList<>();
        for (int l = 1; l <= maxLevel; l++) {
            pool.add(new Entry("fish" + l, l, 10));
        }
        return pool;
    }

    private static Map<Integer, Integer> run(List<Entry> pool, int target, boolean hasBait, float luck,
            String baitId, long seed) {
        Random r = new Random(seed);
        Map<Integer, Integer> counts = new HashMap<>();
        for (int i = 0; i < N; i++) {
            Entry e = CatchSelector.select(pool, target, hasBait, luck, baitId, r);
            counts.merge(e.level(), 1, Integer::sum);
        }
        return counts;
    }

    private static double share(Map<Integer, Integer> counts, int level) {
        return counts.getOrDefault(level, 0) / (double) N;
    }

    @Test
    void emptyPoolReturnsNull() {
        assertNull(CatchSelector.select(List.of(), 1, false, 0f, null, new Random(1)));
    }

    @Test
    void noBaitOnlyGivesLevelOneEvenWithHighLuck() {
        Map<Integer, Integer> c = run(oneFishPerLevel(5), 1, false, 50f, null, 1);
        assertEquals(N, c.getOrDefault(1, 0));
    }

    @Test
    void baitedCatchFollowsUpChanceAndHalvingFalloff() {
        // T=3: 10% one level up (4), otherwise 3:2:1 = 1 : 0.5 : 0.25 of the remaining 90%.
        Map<Integer, Integer> c = run(oneFishPerLevel(5), 3, true, 0f, null, 2);
        assertEquals(0.10, share(c, 4), 0.015);
        assertEquals(0.90 * (1.0 / 1.75), share(c, 3), 0.02);
        assertEquals(0.90 * (0.5 / 1.75), share(c, 2), 0.02);
        assertEquals(0.90 * (0.25 / 1.75), share(c, 1), 0.02);
        assertEquals(0.0, share(c, 5), 0.0);
    }

    @Test
    void luckRaisesUpChanceAndIsCapped() {
        assertEquals(0.10, CatchSelector.upChance(true, 0f), 1e-9);
        assertEquals(0.25, CatchSelector.upChance(true, 5f), 1e-9);
        assertEquals(0.5, CatchSelector.upChance(true, 100f), 1e-9);
        assertEquals(0.10, CatchSelector.upChance(true, -3f), 1e-9);
        assertEquals(0.0, CatchSelector.upChance(false, 100f), 1e-9);
    }

    @Test
    void levelsWithoutEntriesAreSkipped() {
        List<Entry> pool = List.of(new Entry("a", 1, 10), new Entry("b", 5, 10));
        // T=3: nothing at 3 or 2, level 4 has no entry so the up roll is wasted; only level 1 is left.
        Map<Integer, Integer> c = run(pool, 3, true, 0f, null, 3);
        assertEquals(N, c.getOrDefault(1, 0));
    }

    @Test
    void poolEntirelyAboveTargetFallsBackToLowestLevel() {
        List<Entry> pool = List.of(new Entry("x", 5, 10), new Entry("y", 8, 10));
        Map<Integer, Integer> c = run(pool, 1, true, 0f, null, 4);
        assertEquals(N, c.getOrDefault(5, 0));
    }

    @Test
    void levelTwentyNeverRollsAbove() {
        List<Entry> pool = oneFishPerLevel(20);
        Map<Integer, Integer> c = run(pool, 20, true, 100f, null, 5);
        for (int level : c.keySet()) {
            assertTrue(level <= 20);
        }
        // Halving falloff from 20 gives level 20 a share of about 0.5 (1 / sum of 0.5^k), so allow sampling noise.
        assertEquals(0.5, share(c, 20), 0.02);
    }

    @Test
    void catchWeightsWithinALevelAreRespected() {
        List<Entry> pool = List.of(new Entry("light", 1, 10), new Entry("heavy", 1, 30));
        Random r = new Random(6);
        int heavy = 0;
        for (int i = 0; i < N; i++) {
            if (CatchSelector.select(pool, 1, false, 0f, null, r).id().equals("heavy")) {
                heavy++;
            }
        }
        assertEquals(0.75, heavy / (double) N, 0.015);
    }

    @Test
    void baitSpeciesIsAlmostNeverCaught() {
        List<Entry> pool = List.of(new Entry("bait", 1, 10), new Entry("other", 1, 10));
        Random r = new Random(7);
        int same = 0;
        for (int i = 0; i < N; i++) {
            if (CatchSelector.select(pool, 1, true, 0f, "bait", r).id().equals("bait")) {
                same++;
            }
        }
        assertTrue(same / (double) N < 0.02, "bait species share was " + same / (double) N);
    }

    @Test
    void baitSpeciesStillReturnsWhenItIsTheOnlyChoice() {
        List<Entry> pool = List.of(new Entry("bait", 1, 10));
        Entry e = CatchSelector.select(pool, 1, true, 0f, "bait", new Random(8));
        assertNotNull(e);
        assertEquals("bait", e.id());
    }
}
