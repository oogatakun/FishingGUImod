package com.example.fishinggui.skill;

import com.example.fishinggui.fight.FishProfile;

/** Numeric effects of skills and fishing level. */
public final class SkillEffects {
    public static final String BITE_SPEED = "bite_speed";
    public static final String DASH_EASE = "dash_ease";
    public static final String DASH_DELAY = "dash_delay";
    public static final String REEL_SPEED = "reel_speed";
    private static final int SKILL_MAX = 5;
    private static final int FISHING_MAX = 20;

    private SkillEffects() {
    }

    private static int clamp(int level, int max) {
        return Math.min(max, Math.max(0, level));
    }

    /** Fraction of the bite wait removed: 0.15 per level, up to level 5 (75%). */
    public static double biteSpeedBonus(int level) {
        return clamp(level, SKILL_MAX) * 0.15;
    }

    /** Expected extra wait-counter decrements per tick so that the wait shrinks by {@code biteSpeedBonus}. */
    public static double biteSpeedExtraTicks(int level) {
        double reduction = biteSpeedBonus(level);
        return reduction / (1.0 - reduction);
    }

    /** Multiplier on the tension a dash adds: 1 - 0.06 per level (Lv5: 0.70). */
    public static double dashEaseScale(int level) {
        return 1.0 - clamp(level, SKILL_MAX) * 0.06;
    }

    /** Multiplier on the time between dashes: 1 + 0.08 per level (Lv5: 1.40). */
    public static double dashDelayScale(int level) {
        return 1.0 + clamp(level, SKILL_MAX) * 0.08;
    }

    /** Multiplier on how fast reeling wears the fish down: 1 + 0.08 per level (Lv5: 1.40). */
    public static double reelSpeedScale(int level) {
        return 1.0 + clamp(level, SKILL_MAX) * 0.08;
    }

    /** Multiplier on the tension reeling adds, from the fishing level: 1 - 0.015 per level above 1 (Lv20: 0.715). */
    public static double lineControlScale(int fishingLevel) {
        return 1.0 - Math.max(0, clamp(fishingLevel, FISHING_MAX) - 1) * 0.015;
    }

    /** Applies the fight skills and the fishing level to a fish's profile. */
    public static FishProfile applyToProfile(FishProfile base, int fishingLevel, int dashEase, int dashDelay,
            int reelSpeed) {
        return new FishProfile(
                (float) (base.stamina() / reelSpeedScale(reelSpeed)),
                Math.max(1, (int) Math.round(base.dashInterval() * dashDelayScale(dashDelay))),
                (float) (base.dashPower() * dashEaseScale(dashEase)),
                (float) (base.tensionGain() * lineControlScale(fishingLevel)));
    }
}
