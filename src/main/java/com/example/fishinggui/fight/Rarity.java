package com.example.fishinggui.fight;

import java.util.Locale;

public enum Rarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC;

    public static Rarity fromName(String name) {
        for (Rarity r : values()) {
            if (r.name().equalsIgnoreCase(name)) {
                return r;
            }
        }
        throw new IllegalArgumentException("unknown rarity '" + name + "'");
    }

    public String langKey() {
        return "gui.fishinggui.rarity." + name().toLowerCase(Locale.ROOT);
    }
}
