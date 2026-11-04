package com.example.fishinggui.food;

import com.example.fishinggui.item.FishIds;
import java.util.HashMap;
import java.util.Map;

/** Shichirin cooking numbers and the raw fish to cooked fish mapping. Pure Java. */
public final class CookingRules {
    public static final int BURN_TICKS = 400;
    public static final int FUEL_TICKS = 1600;
    public static final int MAX_FUEL = 4;

    private static final String MOD = "fishinggui:";
    private static final Map<String, String> COOKED = new HashMap<>();

    static {
        COOKED.put("minecraft:cod", "minecraft:cooked_cod");
        COOKED.put("minecraft:salmon", "minecraft:cooked_salmon");
        for (String id : FishIds.ALL) {
            COOKED.put(MOD + id, MOD + "cooked_" + id);
        }
    }

    private CookingRules() {
    }

    /** Ticks to grill a fish of this level: 200 + 20 x (level - 1). */
    public static int cookTicks(int level) {
        return 200 + 20 * (level - 1);
    }

    /** Full resource string of the cooked result, or null if not grillable. Accepts ids without namespace. */
    public static String cookedIdFor(String rawId) {
        if (rawId == null) {
            return null;
        }
        String id = rawId;
        if (id.indexOf(':') < 0) {
            id = (id.equals("cod") || id.equals("salmon")) ? "minecraft:" + id : MOD + id;
        }
        return COOKED.get(id);
    }

    public static boolean isGrillable(String rawId) {
        return cookedIdFor(rawId) != null;
    }
}
