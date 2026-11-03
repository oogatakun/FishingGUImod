package com.example.fishinggui.skill;

import com.example.fishinggui.fight.Rarity;

/** How many skill points converting a catch of the given rarity yields. */
public final class PointRules {
    private PointRules() {
    }

    public static int pointsFor(Rarity rarity) {
        switch (rarity) {
            case UNCOMMON:
                return 2;
            case RARE:
                return 3;
            case EPIC:
                return 4;
            case COMMON:
            default:
                return 1;
        }
    }
}
