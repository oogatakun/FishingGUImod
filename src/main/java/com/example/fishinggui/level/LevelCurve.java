package com.example.fishinggui.level;

/** XP needed to go from level n to n+1, for n in 1..MAX_LEVEL-1. */
public final class LevelCurve {
    public static final int MAX_LEVEL = 20;
    public static final LevelCurve DEFAULT = defaultCurve();

    private final int[] xpToNext;

    private LevelCurve(int[] xpToNext) {
        this.xpToNext = xpToNext;
    }

    public static LevelCurve of(int[] values) {
        if (values.length != MAX_LEVEL - 1) {
            throw new IllegalArgumentException("xp_to_next must have " + (MAX_LEVEL - 1) + " entries");
        }
        for (int v : values) {
            if (v < 1) {
                throw new IllegalArgumentException("xp_to_next entries must be >= 1");
            }
        }
        return new LevelCurve(values.clone());
    }

    public int xpToNext(int level) {
        if (level >= MAX_LEVEL) {
            return 0;
        }
        return xpToNext[Math.max(1, level) - 1];
    }

    private static LevelCurve defaultCurve() {
        int[] v = new int[MAX_LEVEL - 1];
        for (int i = 0; i < v.length; i++) {
            v[i] = 50 * (i + 1);
        }
        return new LevelCurve(v);
    }
}
