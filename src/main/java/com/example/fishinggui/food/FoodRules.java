package com.example.fishinggui.food;

import com.example.fishinggui.fight.Rarity;

/** Cooked fish food values derived from the fish level and rarity. Pure Java. */
public final class FoodRules {
    private FoodRules() {
    }

    /** Hunger points: 3 + floor(0.3 x (level - 1)). */
    public static int nutrition(int level) {
        return 3 + (int) Math.floor(0.3 * (level - 1) + 1e-9);
    }

    /** Saturation modifier: 0.6 + 0.01 x (level - 1). */
    public static float saturationModifier(int level) {
        return 0.6f + 0.01f * (level - 1);
    }

    /** Instant heal in half-hearts: common 0, uncommon 1, rare 2, epic 4. */
    public static int heal(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> 0;
            case UNCOMMON -> 1;
            case RARE -> 2;
            case EPIC -> 4;
        };
    }
}
