package com.example.fishinggui.food;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.fishinggui.item.FishIds;
import org.junit.jupiter.api.Test;

class CookingRulesTest {
    @Test
    void cookTicks() {
        assertEquals(200, CookingRules.cookTicks(1));
        assertEquals(580, CookingRules.cookTicks(20));
    }

    @Test
    void constants() {
        assertEquals(400, CookingRules.BURN_TICKS);
        assertEquals(1600, CookingRules.FUEL_TICKS);
        assertEquals(4, CookingRules.MAX_FUEL);
    }

    @Test
    void originalFishCookToModItems() {
        assertEquals("fishinggui:cooked_aji", CookingRules.cookedIdFor("fishinggui:aji"));
        assertEquals("fishinggui:cooked_aji", CookingRules.cookedIdFor("aji"));
        assertTrue(CookingRules.isGrillable("aji"));
        assertTrue(CookingRules.isGrillable("fishinggui:manbo"));
    }

    @Test
    void allOriginalsAreGrillable() {
        for (String id : FishIds.ALL) {
            assertEquals("fishinggui:cooked_" + id, CookingRules.cookedIdFor(id));
            assertTrue(CookingRules.isGrillable("fishinggui:" + id));
        }
    }

    @Test
    void vanillaCodAndSalmon() {
        assertEquals("minecraft:cooked_cod", CookingRules.cookedIdFor("minecraft:cod"));
        assertEquals("minecraft:cooked_salmon", CookingRules.cookedIdFor("minecraft:salmon"));
        assertEquals("minecraft:cooked_cod", CookingRules.cookedIdFor("cod"));
        assertTrue(CookingRules.isGrillable("minecraft:cod"));
    }

    @Test
    void notGrillable() {
        for (String id : new String[] { "minecraft:tropical_fish", "minecraft:pufferfish", "fishinggui:worm",
                "minecraft:stick", "nonsense", "", "fishinggui:cooked_aji" }) {
            assertNull(CookingRules.cookedIdFor(id), id);
            assertFalse(CookingRules.isGrillable(id), id);
        }
        assertNull(CookingRules.cookedIdFor(null));
        assertFalse(CookingRules.isGrillable(null));
    }
}
