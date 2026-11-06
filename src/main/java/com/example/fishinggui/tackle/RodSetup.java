package com.example.fishinggui.tackle;

import com.example.fishinggui.fight.FishProfile;

/** The parts fitted to a rod (rank 0 = nothing fitted) and the numbers they add up to. Pure Java. */
public final class RodSetup {
    private static final int[] LEVEL_CAP = {0, 5, 10, 15, 20};
    private static final int[] DURABILITY = {0, 128, 320, 800, 2000};
    private static final double[] REEL_SPEED = {1.0, 1.10, 1.20, 1.30, 1.40};
    private static final double[] TENSION = {1.0, 0.94, 0.88, 0.82, 0.76};
    private static final float[] HOOK_LUCK = {0f, 1f, 2f, 3f, 4f};
    private static final double[] BITE_CUT = {0.0, 0.05, 0.10, 0.15, 0.20};
    private static final double[] DASH = {1.0, 0.95, 0.90, 0.85, 0.80};

    private final int[] ranks = new int[Part.values().length];

    public RodSetup() {
    }

    public RodSetup(int[] ranks) {
        for (int i = 0; i < this.ranks.length && i < ranks.length; i++) {
            this.ranks[i] = Math.max(0, Math.min(Part.MAX_RANK, ranks[i]));
        }
    }

    public RodSetup with(Part part, int rank) {
        RodSetup next = new RodSetup(ranks);
        next.ranks[part.ordinal()] = Math.max(0, Math.min(Part.MAX_RANK, rank));
        return next;
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
        return DURABILITY[rank(Part.ROD_BLANK)];
    }

    public static int durabilityOf(int rodRank) {
        return DURABILITY[Math.max(0, Math.min(Part.MAX_RANK, rodRank))];
    }

    /** Multiplier on how fast reeling wears the fish down. */
    public double reelSpeed() {
        return REEL_SPEED[rank(Part.REEL)];
    }

    /** Multiplier on the tension reeling adds. */
    public double tensionScale() {
        return TENSION[rank(Part.LINE)];
    }

    /** Added to the luck that decides the chance of a fish one level up. */
    public float hookLuck() {
        return HOOK_LUCK[rank(Part.HOOK)];
    }

    /** Fraction of the bite wait removed by the float. */
    public double biteCut() {
        return BITE_CUT[rank(Part.FLOAT)];
    }

    /** Multiplier on the tension a dash adds, from the leader. */
    public double dashScale() {
        return DASH[rank(Part.LEADER)];
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
