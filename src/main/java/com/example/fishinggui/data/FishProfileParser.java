package com.example.fishinggui.data;

import com.example.fishinggui.fight.FishInfo;
import com.example.fishinggui.fight.FishProfile;
import com.example.fishinggui.fight.FoodOverride;
import com.example.fishinggui.fight.Rarity;
import com.example.fishinggui.level.LevelCurve;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/** Validates one fish-profile JSON object. Pure Java apart from Gson so it stays unit-testable. */
public final class FishProfileParser {
    public record Entry(String item, FishProfile profile, FishInfo info) {
    }

    private FishProfileParser() {
    }

    public static Entry parse(JsonObject json) {
        String item = string(json, "item");
        FishProfile d = FishProfile.DEFAULT;
        float stamina = number(json, "stamina", d.stamina());
        float dashInterval = number(json, "dash_interval", d.dashInterval());
        float dashPower = number(json, "dash_power", d.dashPower());
        float tensionGain = number(json, "tension_gain", d.tensionGain());
        if (stamina <= 0f) {
            throw new IllegalArgumentException("stamina must be > 0");
        }
        if (dashInterval < 1f || dashInterval != Math.floor(dashInterval)) {
            throw new IllegalArgumentException("dash_interval must be an integer >= 1");
        }
        if (dashPower < 0f) {
            throw new IllegalArgumentException("dash_power must be >= 0");
        }
        if (tensionGain <= 0f) {
            throw new IllegalArgumentException("tension_gain must be > 0");
        }
        FishInfo di = FishInfo.DEFAULT;
        Rarity rarity = di.rarity();
        if (json.has("rarity")) {
            rarity = Rarity.fromName(string(json, "rarity"));
        }
        float weightMin = number(json, "weight_min", di.weightMin());
        float weightMax = number(json, "weight_max", di.weightMax());
        float xp = number(json, "xp", di.xp());
        if (weightMin <= 0f) {
            throw new IllegalArgumentException("weight_min must be > 0");
        }
        if (weightMax < weightMin) {
            throw new IllegalArgumentException("weight_max must be >= weight_min");
        }
        if (xp < 0f || xp != Math.floor(xp)) {
            throw new IllegalArgumentException("xp must be an integer >= 0");
        }
        int catchLevel = integer(json, "level", 0, 1, LevelCurve.MAX_LEVEL);
        int catchWeight = integer(json, "catch_weight", 10, 1, Integer.MAX_VALUE);
        int baitLevel = integer(json, "bait_level", 0, 1, LevelCurve.MAX_LEVEL);
        return new Entry(item, new FishProfile(stamina, (int) dashInterval, dashPower, tensionGain),
                new FishInfo(rarity, weightMin, weightMax, (int) xp, catchLevel, catchWeight, baitLevel,
                        food(json)));
    }

    private static FoodOverride food(JsonObject json) {
        JsonElement e = json.get("food");
        if (e == null) {
            return null;
        }
        if (!e.isJsonObject()) {
            throw new IllegalArgumentException("'food' must be an object");
        }
        JsonObject f = e.getAsJsonObject();
        for (String key : new String[] { "nutrition", "saturation", "heal" }) {
            if (!f.has(key)) {
                throw new IllegalArgumentException("'food' requires '" + key + "'");
            }
        }
        float nutrition = number(f, "nutrition", 0f);
        float saturation = number(f, "saturation", 0f);
        float heal = number(f, "heal", 0f);
        if (nutrition < 0f || nutrition != Math.floor(nutrition)) {
            throw new IllegalArgumentException("food.nutrition must be an integer >= 0");
        }
        if (saturation < 0f) {
            throw new IllegalArgumentException("food.saturation must be >= 0");
        }
        if (heal < 0f || heal != Math.floor(heal)) {
            throw new IllegalArgumentException("food.heal must be an integer >= 0");
        }
        return new FoodOverride((int) nutrition, saturation, (int) heal);
    }

    /** Optional integer in [min, max]; {@code fallback} when absent (the fallback itself is not range-checked). */
    private static int integer(JsonObject json, String key, int fallback, int min, int max) {
        if (!json.has(key)) {
            return fallback;
        }
        float v = number(json, key, fallback);
        if (v != Math.floor(v) || v < min || v > max) {
            throw new IllegalArgumentException("'" + key + "' must be an integer in " + min + ".." + max);
        }
        return (int) v;
    }

    private static String string(JsonObject json, String key) {
        JsonElement e = json.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("'" + key + "' must be a string");
        }
        return e.getAsString();
    }

    private static float number(JsonObject json, String key, float fallback) {
        JsonElement e = json.get(key);
        if (e == null) {
            return fallback;
        }
        if (!e.isJsonPrimitive()) {
            throw new IllegalArgumentException("'" + key + "' must be a number");
        }
        JsonPrimitive p = e.getAsJsonPrimitive();
        if (!p.isNumber()) {
            throw new IllegalArgumentException("'" + key + "' must be a number");
        }
        return p.getAsFloat();
    }
}
