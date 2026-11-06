package com.example.fishinggui.fight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class JunkPoolTest {
    private static final List<CatchSelector.Entry> POOL = List.of(
            new CatchSelector.Entry("minecraft:stick", 1, 4),
            new CatchSelector.Entry("minecraft:ink_sac", 3, 4),
            new CatchSelector.Entry("minecraft:saddle", 12, 10),
            new CatchSelector.Entry("fishinggui:aji", 1, 10),
            new CatchSelector.Entry("fishinggui:octopus", 2, 8));

    @Test
    void junkIsOnlyLowLevelVanillaItems() {
        List<CatchSelector.Entry> junk = CatchSelector.junk(POOL);
        assertEquals(2, junk.size());
        assertTrue(junk.stream().allMatch(e -> e.id().startsWith("minecraft:") && e.level() <= 3));
    }

    @Test
    void aVanillaRodNeverLandsAFishOrTreasure() {
        Random random = new Random(7);
        for (int i = 0; i < 500; i++) {
            CatchSelector.Entry e = CatchSelector.select(CatchSelector.junk(POOL), CatchSelector.JUNK_MAX_LEVEL,
                    false, 0f, null, random);
            assertTrue(e.id().equals("minecraft:stick") || e.id().equals("minecraft:ink_sac"), e.id());
        }
    }
}
