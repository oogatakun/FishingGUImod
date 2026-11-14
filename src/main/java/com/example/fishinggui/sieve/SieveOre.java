package com.example.fishinggui.sieve;

/**
 * The ores a sieve turns up. A sift that isn't empty ({@link SieveDrops#NOTHING_CHANCE}) rolls every ore on
 * its own with {@code chance}, so one sift can give several kinds, and a hit gives between {@code min} and
 * {@code max} bits.
 */
public enum SieveOre {
    COAL("coal", 0.50F, 1, 3),
    COPPER("copper", 0.45F, 1, 3),
    IRON("iron", 0.35F, 1, 3),
    REDSTONE("redstone", 0.30F, 1, 3),
    LAPIS("lapis", 0.25F, 1, 3),
    GOLD("gold", 0.20F, 1, 2),
    DIAMOND("diamond", 0.04F, 1, 2),
    EMERALD("emerald", 0.05F, 1, 2);

    public final String id;
    public final float chance;
    public final int min;
    public final int max;

    SieveOre(String id, float chance, int min, int max) {
        this.id = id;
        this.chance = chance;
        this.min = min;
        this.max = max;
    }
}
