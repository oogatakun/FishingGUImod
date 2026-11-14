package com.example.fishinggui.sieve;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import net.minecraft.util.RandomSource;

/**
 * Checks the sieve's drop roll against the table in {@link SieveOre} over a very large number of sifts, and
 * measures how many sifts a player needs for one finished item (81 bits).
 */
class SieveDropsTest {
    private static final int SIFTS = 2_000_000;
    /** Allowed distance from the expected value, in standard errors. */
    private static final double MAX_Z = 5.0;

    @Test
    void ratesCountsAndIndependenceMatchTheTable() {
        RandomSource random = RandomSource.create(20261114L);
        SieveOre[] ores = SieveOre.values();
        long[] hits = new long[ores.length];
        long[] bits = new long[ores.length];
        long[][] countHistogram = new long[ores.length][4];
        long worms = 0;
        long[] kindsPerSift = new long[ores.length + 1];

        for (int i = 0; i < SIFTS; i++) {
            SieveDrops.Result r = SieveDrops.roll(random);
            int kinds = 0;
            for (SieveOre ore : ores) {
                int n = r.bits()[ore.ordinal()];
                if (n > 0) {
                    hits[ore.ordinal()]++;
                    bits[ore.ordinal()] += n;
                    countHistogram[ore.ordinal()][n]++;
                    kinds++;
                }
            }
            kindsPerSift[kinds]++;
            if (r.worm()) {
                worms++;
            }
        }

        System.out.printf("%n=== %,d sifts ===%n", SIFTS);
        System.out.printf("%-9s %8s %8s %6s   %-22s %9s %11s%n",
                "ore", "per sift", "measured", "z", "bits per hit (1/2/3)", "bits/sift", "sifts/item");
        for (SieveOre ore : ores) {
            int o = ore.ordinal();
            double p = SieveDrops.hitChance(ore);
            double rate = (double) hits[o] / SIFTS;
            double z = (rate - p) / Math.sqrt(p * (1 - p) / SIFTS);
            double perSift = (double) bits[o] / SIFTS;
            int span = ore.max - ore.min + 1;
            StringBuilder shares = new StringBuilder();
            for (int n = 1; n <= 3; n++) {
                if (n < ore.min || n > ore.max) {
                    shares.append("  -   ");
                    continue;
                }
                double share = (double) countHistogram[o][n] / hits[o];
                shares.append(String.format("%5.1f%% ", share * 100));
                // Each count in min..max should come up equally often.
                double q = 1.0 / span;
                double zc = (share - q) / Math.sqrt(q * (1 - q) / hits[o]);
                assertTrue(Math.abs(zc) < MAX_Z, ore + " count " + n + " share off by z=" + zc);
            }
            System.out.printf("%-9s %7.2f%% %7.2f%% %+6.2f   %-22s %9.3f %11.1f%n",
                    ore.id, p * 100, rate * 100, z, shares, perSift, 81 / perSift);
            assertTrue(Math.abs(z) < MAX_Z, ore + " rate off by z=" + z);
        }
        double wormRate = (double) worms / SIFTS;
        double wp = SieveDrops.wormChance();
        double wz = (wormRate - wp) / Math.sqrt(wp * (1 - wp) / SIFTS);
        System.out.printf("%-9s %7.2f%% %7.2f%% %+6.2f%n", "worm", wp * 100, wormRate * 100, wz);
        assertTrue(Math.abs(wz) < MAX_Z, "worm rate off by z=" + wz);

        // If the ores are rolled independently, the number of kinds per sift follows from the chances alone.
        double[] expectedKinds = new double[ores.length + 1];
        expectedKinds[0] = 1.0;
        for (SieveOre ore : ores) {
            for (int k = ores.length; k >= 0; k--) {
                expectedKinds[k] = expectedKinds[k] * (1 - ore.chance) + (k > 0 ? expectedKinds[k - 1] * ore.chance : 0);
            }
        }
        // The empty roll adds its share to "no ore at all"; the rest is spread as above.
        for (int k = 0; k <= ores.length; k++) {
            expectedKinds[k] *= 1 - SieveDrops.NOTHING_CHANCE;
        }
        expectedKinds[0] += SieveDrops.NOTHING_CHANCE;
        System.out.printf("%nkinds of ore in one sift:%n");
        for (int k = 0; k <= 5; k++) {
            double measured = (double) kindsPerSift[k] / SIFTS;
            double e = expectedKinds[k];
            double z = (measured - e) / Math.sqrt(e * (1 - e) / SIFTS);
            System.out.printf("  %d kinds: expected %6.2f%%  measured %6.2f%%  z %+5.2f%n", k, e * 100, measured * 100, z);
            assertTrue(Math.abs(z) < MAX_Z, k + " kinds off by z=" + z);
        }
    }

    @Test
    void siftsNeededForOneFinishedItem() {
        RandomSource random = RandomSource.create(42L);
        SieveOre[] ores = SieveOre.values();
        int players = 5_000;
        int[][] needed = new int[ores.length][players];
        for (int p = 0; p < players; p++) {
            int[] have = new int[ores.length];
            int done = 0;
            for (int sift = 1; done < ores.length; sift++) {
                SieveDrops.Result r = SieveDrops.roll(random);
                for (int o = 0; o < ores.length; o++) {
                    if (have[o] < 81) {
                        have[o] += r.bits()[o];
                        if (have[o] >= 81) {
                            needed[o][p] = sift;
                            done++;
                        }
                    }
                }
            }
        }
        System.out.printf("%nsifts until the first finished item (%,d simulated players, 1.5 s per sift):%n", players);
        System.out.printf("%-9s %7s %7s %7s %7s %12s%n", "ore", "luckiest", "10%", "median", "90%", "median time");
        for (SieveOre ore : ores) {
            int[] n = needed[ore.ordinal()];
            Arrays.sort(n);
            int median = n[players / 2];
            System.out.printf("%-9s %7d %7d %7d %7d %9.1f min%n",
                    ore.id, n[0], n[players / 10], median, n[players * 9 / 10], median * 1.5 / 60);
        }
    }
}
