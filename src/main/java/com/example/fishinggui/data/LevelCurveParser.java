package com.example.fishinggui.data;

import com.example.fishinggui.level.LevelCurve;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Validates one fishing_levels JSON object. */
public final class LevelCurveParser {
    private LevelCurveParser() {
    }

    public static LevelCurve parse(JsonObject json) {
        JsonElement e = json.get("xp_to_next");
        if (e == null || !e.isJsonArray()) {
            throw new IllegalArgumentException("'xp_to_next' must be an array");
        }
        JsonArray array = e.getAsJsonArray();
        int[] values = new int[array.size()];
        for (int i = 0; i < values.length; i++) {
            JsonElement item = array.get(i);
            if (!item.isJsonPrimitive() || !item.getAsJsonPrimitive().isNumber()) {
                throw new IllegalArgumentException("xp_to_next[" + i + "] must be a number");
            }
            double d = item.getAsDouble();
            if (d != Math.floor(d) || d > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("xp_to_next[" + i + "] must be an integer");
            }
            values[i] = (int) d;
        }
        return LevelCurve.of(values);
    }
}
