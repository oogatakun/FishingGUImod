package com.example.fishinggui.level;

/** A player's fishing level and the XP collected towards the next one. */
public record FishingProgress(int level, int xp) {
    public static final FishingProgress START = new FishingProgress(1, 0);

    public FishingProgress {
        level = Math.max(1, Math.min(LevelCurve.MAX_LEVEL, level));
        xp = level >= LevelCurve.MAX_LEVEL ? 0 : Math.max(0, xp);
    }

    public boolean atMax() {
        return level >= LevelCurve.MAX_LEVEL;
    }

    public FishingProgress addXp(int amount, LevelCurve curve) {
        if (amount <= 0 || atMax()) {
            return this;
        }
        int lv = level;
        long total = (long) xp + amount;
        while (lv < LevelCurve.MAX_LEVEL && total >= curve.xpToNext(lv)) {
            total -= curve.xpToNext(lv);
            lv++;
        }
        return new FishingProgress(lv, lv >= LevelCurve.MAX_LEVEL ? 0 : (int) total);
    }

    public FishingProgress withLevel(int newLevel) {
        return new FishingProgress(newLevel, 0);
    }

    public FishingProgress withXp(int newXp, LevelCurve curve) {
        return new FishingProgress(level, 0).addXp(Math.max(0, newXp), curve);
    }
}
