package com.example.fishinggui.entity;

/** The numbers behind worm breeding. Pure Java. */
public final class WormBreeding {
    /** Ticks a worm waits after breeding (5 minutes). */
    public static final int COOLDOWN = 6000;
    /** Ticks a worm that has just been put down, or just been born, waits before it can breed (1 minute). */
    public static final int SETTLE = 1200;
    /** Two worms this close (blocks) can breed. */
    public static final double PARTNER_RANGE = 5.0;
    /** Worms within this many blocks of the pair count against the cap. */
    public static final double CROWD_RANGE = 8.0;
    /** No pair breeds while this many worms already share the area. */
    public static final int CROWD_CAP = 8;
    /** Each breeding brings this many young. */
    public static final int OFFSPRING = 2;

    private WormBreeding() {
    }

    /** True if an area that already holds {@code nearby} worms (the pair included) has room for the young. */
    public static boolean roomFor(int nearby) {
        return nearby + OFFSPRING <= CROWD_CAP;
    }
}
