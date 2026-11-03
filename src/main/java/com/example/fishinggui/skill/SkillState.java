package com.example.fishinggui.skill;

import java.util.HashMap;
import java.util.Map;

/** Immutable skill points plus learned skill levels. */
public record SkillState(int points, Map<String, Integer> skills) {
    public static final SkillState START = new SkillState(0, Map.of());

    public SkillState {
        points = Math.max(0, points);
        skills = Map.copyOf(skills);
    }

    public SkillState addPoints(int delta) {
        return withPoints(points + delta);
    }

    public SkillState withPoints(int newPoints) {
        return new SkillState(newPoints, skills);
    }

    public int levelOf(String id) {
        return skills.getOrDefault(id, 0);
    }

    public boolean canBuy(SkillDef def) {
        int cost = def.nextCost(levelOf(def.id()));
        return cost > 0 && points >= cost;
    }

    public SkillState buy(SkillDef def) {
        if (!canBuy(def)) {
            throw new IllegalStateException("cannot buy skill " + def.id());
        }
        int level = levelOf(def.id());
        Map<String, Integer> next = new HashMap<>(skills);
        next.put(def.id(), level + 1);
        return new SkillState(points - def.nextCost(level), next);
    }
}
