package com.example.fishinggui.tackle;

import com.example.fishinggui.fight.FishProfile;

/**
 * The parts fitted to a rod (rank 0 = nothing fitted, plus the style of each part) and the numbers they add up to.
 * Parts weigh something; the rod blank carries only so much, and every point over that slows the reel, dulls the
 * float and strains the line. Pure Java.
 */
public final class RodSetup {
    private static final int[] LEVEL_CAP = {0, 5, 10, 15, 20};
    private static final int[] DURABILITY = {0, 128, 320, 800, 2000};
    private static final double[] REEL_SPEED = {1.0, 1.10, 1.20, 1.30, 1.40};
    private static final double[] TENSION = {1.0, 0.94, 0.88, 0.82, 0.76};
    private static final float[] HOOK_LUCK = {0f, 1f, 2f, 3f, 4f};
    private static final double[] BITE_CUT = {0.0, 0.05, 0.10, 0.15, 0.20};
    private static final double[] DASH = {1.0, 0.95, 0.90, 0.85, 0.80};

    /** What a rod blank of rank 1..4 can carry, in weight points. */
    private static final int[] CAPACITY = {0, 24, 28, 32, 36};
    /** A part weighs this much per rank, before its style. */
    public static final int WEIGHT_PER_RANK = 2;
    /** What each weight point over the capacity costs. */
    public static final double REEL_LOSS_PER_OVER = 0.025;
    public static final double BITE_LOSS_PER_OVER = 0.005;
    public static final double TENSION_GAIN_PER_OVER = 0.01;
    private static final double MIN_REEL_FACTOR = 0.4;
    /** A style's power counts this many times over for the rod blank's durability. */
    private static final double DURABILITY_PER_POWER = 3.0;
    /** A style's weight change counts this many times over for the rod blank's capacity. */
    private static final int CAPACITY_PER_WEIGHT = 2;

    /** Chance per catch that a fitted lure calls its species. */
    public static final double LURE_CHANCE = 0.10;
    /** A style's power counts this many times over for the lure's chance. */
    private static final double LURE_PER_POWER = 2.0;
    /** The highest fish level a lure of rank 1..4 can call: 5 per rank. */
    public static final int LURE_LEVELS_PER_RANK = 5;

    private final int[] ranks = new int[Part.values().length];
    private final Style[] styles = new Style[Part.values().length];
    private String lureSpecies;

    public RodSetup() {
    }

    public RodSetup(int[] ranks) {
        for (int i = 0; i < this.ranks.length && i < ranks.length; i++) {
            this.ranks[i] = Math.max(0, Math.min(Part.MAX_RANK, ranks[i]));
        }
    }

    private RodSetup copy() {
        RodSetup next = new RodSetup(ranks);
        System.arraycopy(styles, 0, next.styles, 0, styles.length);
        next.lureSpecies = lureSpecies;
        return next;
    }

    /** The same setup with the part at {@code rank}, in its plain form. */
    public RodSetup with(Part part, int rank) {
        return with(part, rank, null);
    }

    /** The same setup with the part at {@code rank} in {@code style}; a style of another rank is ignored. */
    public RodSetup with(Part part, int rank, Style style) {
        RodSetup next = copy();
        int r = Math.max(0, Math.min(Part.MAX_RANK, rank));
        next.ranks[part.ordinal()] = r;
        next.styles[part.ordinal()] = style != null && style.rank() == r ? style : null;
        return next;
    }

    /** The same setup with the species its lure calls (an item id, or null). */
    public RodSetup withLureSpecies(String species) {
        RodSetup next = copy();
        next.lureSpecies = species;
        return next;
    }

    public String lureSpecies() {
        return lureSpecies;
    }

    /** The style of a fitted part, or null for the plain part. */
    public Style style(Part part) {
        return styles[part.ordinal()];
    }

    private double power(Part part) {
        Style s = styles[part.ordinal()];
        return s == null ? 0.0 : s.power();
    }

    /** Chance that the fitted lure calls its species on a catch. */
    public double lureChance() {
        return LURE_CHANCE * (1.0 + LURE_PER_POWER * power(Part.LURE));
    }

    /** True if this catch should be the lure's species: {@code roll} is a uniform 0..1 draw. */
    public boolean lureCalls(double roll, int speciesLevel) {
        int rank = rank(Part.LURE);
        return lureSpecies != null && rank > 0 && roll < lureChance() && speciesLevel > 0
                && speciesLevel <= rank * LURE_LEVELS_PER_RANK;
    }

    public int rank(Part part) {
        return ranks[part.ordinal()];
    }

    /** True when every required part is fitted. */
    public boolean complete() {
        for (Part p : Part.values()) {
            if (p.required() && ranks[p.ordinal()] == 0) {
                return false;
            }
        }
        return true;
    }

    /** Highest bait/fish level the rod can work with. */
    public int levelCap() {
        return LEVEL_CAP[rank(Part.ROD_BLANK)];
    }

    public int durability() {
        return durabilityOf(rank(Part.ROD_BLANK), style(Part.ROD_BLANK));
    }

    public static int durabilityOf(int rodRank) {
        return DURABILITY[Math.max(0, Math.min(Part.MAX_RANK, rodRank))];
    }

    public static int durabilityOf(int rodRank, Style style) {
        double power = style == null ? 0.0 : style.power();
        return (int) Math.round(durabilityOf(rodRank) * (1.0 + DURABILITY_PER_POWER * power));
    }

    /** What one part weighs: its rank and style; 0 when not fitted. */
    public static int weightOf(int rank, Style style) {
        return rank == 0 ? 0 : Math.max(1, WEIGHT_PER_RANK * rank + (style == null ? 0 : style.weightDelta()));
    }

    /** The weight of everything fitted to the rod blank. */
    public int weight() {
        int sum = 0;
        for (Part p : Part.values()) {
            if (p != Part.ROD_BLANK) {
                sum += weightOf(rank(p), style(p));
            }
        }
        return sum;
    }

    /** What the rod blank can carry; 0 with no rod blank. */
    public int capacity() {
        int rod = rank(Part.ROD_BLANK);
        if (rod == 0) {
            return 0;
        }
        Style style = style(Part.ROD_BLANK);
        return Math.max(1, CAPACITY[rod] + (style == null ? 0 : CAPACITY_PER_WEIGHT * style.weightDelta()));
    }

    /** Weight points over the capacity; 0 with no rod blank or when everything fits. */
    public int overweight() {
        return rank(Part.ROD_BLANK) == 0 ? 0 : Math.max(0, weight() - capacity());
    }

    /** Multiplier on how fast reeling wears the fish down. */
    public double reelSpeed() {
        double base = 1.0 + (REEL_SPEED[rank(Part.REEL)] - 1.0) * (1.0 + power(Part.REEL));
        return base * Math.max(MIN_REEL_FACTOR, 1.0 - REEL_LOSS_PER_OVER * overweight());
    }

    /** Multiplier on the tension reeling adds. */
    public double tensionScale() {
        double base = 1.0 - (1.0 - TENSION[rank(Part.LINE)]) * (1.0 + power(Part.LINE));
        return base * (1.0 + TENSION_GAIN_PER_OVER * overweight());
    }

    /** Added to the luck that decides the chance of a fish one level up. */
    public float hookLuck() {
        return (float) (HOOK_LUCK[rank(Part.HOOK)] * (1.0 + power(Part.HOOK)));
    }

    /** Fraction of the bite wait removed by the float; never below nothing. */
    public double biteCut() {
        double cut = BITE_CUT[rank(Part.FLOAT)] * (1.0 + power(Part.FLOAT)) - BITE_LOSS_PER_OVER * overweight();
        return Math.max(0.0, cut);
    }

    /** Multiplier on the tension a dash adds, from the leader. */
    public double dashScale() {
        return 1.0 - (1.0 - DASH[rank(Part.LEADER)]) * (1.0 + power(Part.LEADER));
    }

    /** Applies the rod's parts to a fish's profile. */
    public FishProfile apply(FishProfile base) {
        return new FishProfile((float) (base.stamina() / reelSpeed()), base.dashInterval(),
                (float) (base.dashPower() * dashScale()), (float) (base.tensionGain() * tensionScale()));
    }

    public int[] ranks() {
        return ranks.clone();
    }
}
