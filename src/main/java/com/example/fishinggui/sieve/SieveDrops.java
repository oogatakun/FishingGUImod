package com.example.fishinggui.sieve;

import net.minecraft.util.RandomSource;

/** What one sift turns up, before it becomes item stacks. Kept free of registries so it can be tested on its own. */
public final class SieveDrops {
    /** Chance that a sift turns up nothing at all, rolled before anything else. */
    public static final float NOTHING_CHANCE = 0.25F;
    /** Chance of a worm on a sift that wasn't empty. */
    public static final float WORM_CHANCE = 0.05F;

    private static final Result NOTHING = new Result(new int[SieveOre.values().length], false);

    /**
     * @param bits bits per ore, indexed by {@link SieveOre#ordinal()}; 0 when that ore missed
     * @param worm whether a worm came up
     */
    public record Result(int[] bits, boolean worm) {
    }

    private SieveDrops() {
    }

    /** First the empty roll; if that misses, every ore is rolled on its own, then the worm. */
    public static Result roll(RandomSource random) {
        if (random.nextFloat() < NOTHING_CHANCE) {
            return NOTHING;
        }
        SieveOre[] ores = SieveOre.values();
        int[] bits = new int[ores.length];
        for (SieveOre ore : ores) {
            if (random.nextFloat() < ore.chance) {
                bits[ore.ordinal()] = ore.min + random.nextInt(ore.max - ore.min + 1);
            }
        }
        return new Result(bits, random.nextFloat() < WORM_CHANCE);
    }

    /** The chance per sift that an ore turns up, counting the empty roll. */
    public static double hitChance(SieveOre ore) {
        return (1.0 - NOTHING_CHANCE) * ore.chance;
    }

    /** The chance per sift of a worm, counting the empty roll. */
    public static double wormChance() {
        return (1.0 - NOTHING_CHANCE) * WORM_CHANCE;
    }
}
