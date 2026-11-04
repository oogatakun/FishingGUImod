package com.example.fishinggui.fight;

import java.util.Random;

/** Per-fish catch data: rarity, weight range (kg) and fishing XP. Pure Java. */
public record FishInfo(Rarity rarity, float weightMin, float weightMax, int xp, int catchLevel, int catchWeight,
        int baitLevel) {
    public static final FishInfo DEFAULT = new FishInfo(Rarity.COMMON, 0.1f, 1.0f, 10);

    /** Fish data without catch-pool or bait data (not catchable, not a bait). */
    public FishInfo(Rarity rarity, float weightMin, float weightMax, int xp) {
        this(rarity, weightMin, weightMax, xp, 0, 10, 0);
    }

    static final float LIGHTEST_TENSION_FACTOR = 0.8f;
    static final float HEAVIEST_TENSION_FACTOR = 1.3f;

    /** Uniform weight in the range, rounded to one decimal. */
    public float rollWeight(Random random) {
        float w = weightMin + random.nextFloat() * (weightMax - weightMin);
        return Math.max(weightMin, Math.min(weightMax, Math.round(w * 10f) / 10f));
    }

    private float weightFactor(float weight) {
        float t = weightMax > weightMin ? (weight - weightMin) / (weightMax - weightMin) : 0.5f;
        t = Math.max(0f, Math.min(1f, t));
        return LIGHTEST_TENSION_FACTOR + (HEAVIEST_TENSION_FACTOR - LIGHTEST_TENSION_FACTOR) * t;
    }

    /** Fishing XP for a catch of this weight: the base XP x0.8 (lightest) to x1.3 (heaviest), at least 1. */
    public int xpForWeight(float weight) {
        return Math.max(1, Math.round(xp * weightFactor(weight)));
    }

    /** Heavier fish raise tension faster; stamina and dashes are untouched. */
    public FishProfile scaleForWeight(FishProfile base, float weight) {
        float factor = weightFactor(weight);
        return new FishProfile(base.stamina(), base.dashInterval(), base.dashPower(), base.tensionGain() * factor);
    }
}
