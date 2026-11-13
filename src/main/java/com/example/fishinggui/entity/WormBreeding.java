package com.example.fishinggui.entity;

/** The numbers behind worm breeding. Pure Java. */
public final class WormBreeding {
    /** Ticks a worm waits after breeding (5 minutes). */
    public static final int COOLDOWN = 6000;
    /** Ticks a worm that has just been put down, or just been born, waits before it can breed (1 minute). */
    public static final int SETTLE = 1200;
    /** Two worms this close (blocks) can breed. */
    public static final double PARTNER_RANGE = 5.0;
    /** Each breeding brings this many young. */
    public static final int OFFSPRING = 2;

    private WormBreeding() {
    }
}
