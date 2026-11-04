package com.example.fishinggui.skill;

/** Pure rules for the skill reset item drop. */
public final class ResetRules {
    public static final double RESET_DROP_CHANCE = 0.015;

    private ResetRules() {
    }

    /** True when {@code random} (uniform in [0,1)) wins the bonus drop. */
    public static boolean rollsDrop(double random) {
        return random < RESET_DROP_CHANCE;
    }
}
