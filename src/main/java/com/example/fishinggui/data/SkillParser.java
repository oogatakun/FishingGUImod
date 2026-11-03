package com.example.fishinggui.data;

import com.example.fishinggui.skill.SkillDef;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/** Validates one skill JSON object. */
public final class SkillParser {
    private SkillParser() {
    }

    public static SkillDef parse(String id, JsonObject json) {
        int maxLevel = integer(json.get("max_level"), "max_level");
        if (maxLevel <= 0) {
            throw new IllegalArgumentException("max_level must be > 0");
        }
        JsonElement costElement = json.get("cost");
        if (costElement == null) {
            throw new IllegalArgumentException("missing cost");
        }
        if (!costElement.isJsonArray()) {
            throw new IllegalArgumentException("cost must be an array");
        }
        JsonArray array = costElement.getAsJsonArray();
        if (array.size() != maxLevel) {
            throw new IllegalArgumentException("cost length " + array.size() + " != max_level " + maxLevel);
        }
        List<Integer> cost = new ArrayList<>();
        for (JsonElement e : array) {
            int c = integer(e, "cost entry");
            if (c <= 0) {
                throw new IllegalArgumentException("cost entries must be > 0");
            }
            cost.add(c);
        }
        return new SkillDef(id, maxLevel, cost);
    }

    private static int integer(JsonElement e, String name) {
        if (e == null) {
            throw new IllegalArgumentException("missing " + name);
        }
        if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(name + " must be a number");
        }
        double v = e.getAsDouble();
        if (v != Math.floor(v)) {
            throw new IllegalArgumentException(name + " must be an integer");
        }
        return (int) v;
    }
}
