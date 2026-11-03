package com.example.fishinggui.skill;

import java.util.List;

/** A skill definition: {@code cost.get(i)} raises the level from i to i+1. */
public record SkillDef(String id, int maxLevel, List<Integer> cost) {
    public SkillDef {
        cost = List.copyOf(cost);
    }

    /** Cost to go from {@code level} to the next one, or -1 when already at max (or invalid). */
    public int nextCost(int level) {
        if (level < 0 || level >= maxLevel || level >= cost.size()) {
            return -1;
        }
        return cost.get(level);
    }
}
