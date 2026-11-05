package com.example.fishinggui.food;

/** Stages and timings of one takoyaki well; shared by the block entity, the renderer and the tests. */
public final class TakoyakiRules {
    public static final int WELLS = 12;
    /** Portions in one finished boat. */
    public static final int PORTIONS = 6;
    /** Ticks a well cooks (powered) before it needs flipping, and again before it is done. */
    public static final int COOK_TICKS = 400;

    public static final int EMPTY = 0;
    /** First batter poured. */
    public static final int BATTER = 1;
    /** Octopus added; waiting for the second pour. */
    public static final int FILLED = 2;
    public static final int COOKING = 3;
    /** Colour has changed: flip it with an ice pick. */
    public static final int FLIP = 4;
    public static final int COOKING_BACK = 5;
    /** Done: add toppings, then collect. */
    public static final int READY = 6;

    public static final int MAYO = 1;
    public static final int AONORI = 2;

    private TakoyakiRules() {
    }

    /** The stage a well moves to when its cooking timer runs out, or the same stage if it is not cooking. */
    public static int afterCooking(int stage) {
        return switch (stage) {
            case COOKING -> FLIP;
            case COOKING_BACK -> READY;
            default -> stage;
        };
    }

    public static boolean isCooking(int stage) {
        return stage == COOKING || stage == COOKING_BACK;
    }
}
