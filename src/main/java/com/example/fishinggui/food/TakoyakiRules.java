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

    /** Tools the pan screen offers, in toolbar order. */
    public static final int TOOL_BATTER = 0;
    public static final int TOOL_OCTOPUS = 1;
    public static final int TOOL_PICK = 2;
    public static final int TOOL_MAYO = 3;
    public static final int TOOL_AONORI = 4;
    public static final int TOOL_BOWL = 5;
    public static final int TOOLS = 6;

    private static final int[] FRESH = {240, 218, 148};
    private static final int[] HALF_DONE = {224, 178, 84};
    private static final int[] DONE = {170, 96, 36};

    private TakoyakiRules() {
    }

    /** RGB of a cooked well (COOKING or later). Each side starts at the freshly-cooked colour and browns. */
    public static int[] color(int stage, float progress) {
        int[] target = stage == COOKING || stage == FLIP ? HALF_DONE : DONE;
        float t = stage == COOKING || stage == COOKING_BACK ? progress : 1f;
        return new int[] {lerp(FRESH[0], target[0], t), lerp(FRESH[1], target[1], t), lerp(FRESH[2], target[2], t)};
    }

    private static int lerp(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
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
