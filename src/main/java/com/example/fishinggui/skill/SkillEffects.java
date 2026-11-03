package com.example.fishinggui.skill;

/** Numeric effects of skills. */
public final class SkillEffects {
    public static final String BITE_SPEED = "bite_speed";
    private static final int BITE_SPEED_MAX = 5;

    private SkillEffects() {
    }

    /** Per-tick chance of an extra wait-counter decrement: 0.1 per level, up to level 5. */
    public static double biteSpeedBonus(int level) {
        return Math.min(BITE_SPEED_MAX, Math.max(0, level)) * 0.1;
    }
}
