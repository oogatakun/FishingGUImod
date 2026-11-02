package com.example.fishinggui.data;

import com.example.fishinggui.fight.FishProfile;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/** Validates one fish-profile JSON object. Pure Java apart from Gson so it stays unit-testable. */
public final class FishProfileParser {
    public record Entry(String item, FishProfile profile) {
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
        return new Entry(item, new FishProfile(stamina, (int) dashInterval, dashPower, tensionGain));
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
