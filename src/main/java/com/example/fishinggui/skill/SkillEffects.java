package com.example.fishinggui.skill;

/** Numeric effects of skills. */
public final class SkillEffects {
    public static final String BITE_SPEED = "bite_speed";
    private static final int BITE_SPEED_MAX = 5;

    private SkillEffects() {
    }

    /** Fraction of the bite wait removed: 0.15 per level, up to level 5 (75%). */
    public static double biteSpeedBonus(int level) {
        return Math.min(BITE_SPEED_MAX, Math.max(0, level)) * 0.15;
    }

    /** Expected extra wait-counter decrements per tick so that the wait shrinks by {@code biteSpeedBonus}. */
    public static double biteSpeedExtraTicks(int level) {
        double reduction = biteSpeedBonus(level);
        return reduction / (1.0 - reduction);
    }
}
