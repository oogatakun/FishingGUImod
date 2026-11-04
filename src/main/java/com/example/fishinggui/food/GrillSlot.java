package com.example.fishinggui.food;

/**
 * Pure state of one fish on the shichirin. {@code progress} counts lit ticks since the fish was placed:
 * raw until cookTicks(level), cooked for BURN_TICKS more, then burnt (progress stops there).
 */
public record GrillSlot(int level, int progress) {
    public enum Phase { RAW, COOKED, BURNT }

    public GrillSlot {
        progress = Math.max(0, progress);
    }

    public static GrillSlot fresh(int level) {
        return new GrillSlot(level, 0);
    }

    private int cookedAt() {
        return CookingRules.cookTicks(level);
    }

    private int burntAt() {
        return cookedAt() + CookingRules.BURN_TICKS;
    }

    public Phase phase() {
        if (progress < cookedAt()) {
            return Phase.RAW;
        }
        return progress < burntAt() ? Phase.COOKED : Phase.BURNT;
    }

    /** Advance by one tick while the burner is lit. */
    public GrillSlot tick() {
        return progress >= burntAt() ? this : new GrillSlot(level, progress + 1);
    }

    /** Advance one tick if lit, otherwise stay put. */
    public GrillSlot tick(boolean lit) {
        return lit ? tick() : this;
    }
}
