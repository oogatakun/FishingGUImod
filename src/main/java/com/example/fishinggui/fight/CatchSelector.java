package com.example.fishinggui.fight;

import com.example.fishinggui.level.LevelCurve;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

/** Picks what a fight will land: target level, a lucky step up, a halving falloff downwards. Pure Java. */
public final class CatchSelector {
    static final double BASE_UP = 0.10;
    static final double UP_PER_LUCK = 0.03;
    static final double UP_CAP = 0.5;
    static final double LEVEL_DECAY = 0.5;
    static final double SAME_SPECIES_KEEP = 0.01;
    static final int MAX_REDRAWS = 10;

    public record Entry(String id, int level, int catchWeight) {
    }

    private CatchSelector() {
    }

    static double upChance(boolean hasBait, float luck) {
        if (!hasBait) {
            return 0.0;
        }
        return Math.max(BASE_UP, Math.min(UP_CAP, BASE_UP + UP_PER_LUCK * luck));
    }

    /** Returns the chosen entry, or null when the pool is empty. */
    public static Entry select(List<Entry> pool, int targetLevel, boolean hasBait, float luck, String baitId,
            Random random) {
        if (pool.isEmpty()) {
            return null;
        }
        Entry picked = null;
        for (int attempt = 0; attempt <= MAX_REDRAWS; attempt++) {
            picked = pickOnce(pool, targetLevel, hasBait, luck, random);
            if (baitId == null || !picked.id().equals(baitId) || random.nextDouble() < SAME_SPECIES_KEEP) {
                return picked;
            }
        }
        return picked;
    }

    private static Entry pickOnce(List<Entry> pool, int target, boolean hasBait, float luck, Random random) {
        int level = chooseLevel(pool, target, hasBait, luck, random);
        List<Entry> atLevel = new ArrayList<>();
        int total = 0;
        for (Entry e : pool) {
            if (e.level() == level) {
                atLevel.add(e);
                total += e.catchWeight();
            }
        }
        int roll = random.nextInt(total);
        for (Entry e : atLevel) {
            roll -= e.catchWeight();
            if (roll < 0) {
                return e;
            }
        }
        return atLevel.get(atLevel.size() - 1);
    }

    private static int chooseLevel(List<Entry> pool, int target, boolean hasBait, float luck, Random random) {
        TreeSet<Integer> levels = new TreeSet<>();
        for (Entry e : pool) {
            levels.add(e.level());
        }
        if (target < LevelCurve.MAX_LEVEL && random.nextDouble() < upChance(hasBait, luck)
                && levels.contains(target + 1)) {
            return target + 1;
        }
        double total = 0.0;
        for (int l : levels) {
            if (l <= target) {
                total += Math.pow(LEVEL_DECAY, target - l);
            }
        }
        if (total == 0.0) {
            return levels.first();
        }
        double roll = random.nextDouble() * total;
        int last = levels.first();
        for (int l : levels) {
            if (l > target) {
                break;
            }
            last = l;
            roll -= Math.pow(LEVEL_DECAY, target - l);
            if (roll < 0) {
                return l;
            }
        }
        return last;
    }
}
