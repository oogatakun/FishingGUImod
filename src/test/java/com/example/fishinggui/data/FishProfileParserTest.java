package com.example.fishinggui.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.fishinggui.fight.FishProfile;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class FishProfileParserTest {
    private static JsonObject json(String s) {
        return JsonParser.parseString(s).getAsJsonObject();
    }

    @Test
    void parsesAllFields() {
        FishProfileParser.Entry e = FishProfileParser.parse(json(
                "{\"item\":\"minecraft:pufferfish\",\"stamina\":60,\"dash_interval\":40,"
                        + "\"dash_power\":1.5,\"tension_gain\":1.2}"));
        assertEquals("minecraft:pufferfish", e.item());
        assertEquals(new FishProfile(60f, 40, 1.5f, 1.2f), e.profile());
    }

    @Test
    void omittedFieldsUseDefaults() {
        FishProfileParser.Entry e = FishProfileParser.parse(json("{\"item\":\"minecraft:cod\",\"stamina\":70}"));
        assertEquals(new FishProfile(70f, FishProfile.DEFAULT.dashInterval(), FishProfile.DEFAULT.dashPower(),
                FishProfile.DEFAULT.tensionGain()), e.profile());
    }

    @Test
    void rejectsMissingItem() {
        assertThrows(IllegalArgumentException.class, () -> FishProfileParser.parse(json("{\"stamina\":70}")));
    }

    @Test
    void rejectsOutOfRangeValues() {
        assertThrows(IllegalArgumentException.class,
                () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"stamina\":0}")));
        assertThrows(IllegalArgumentException.class,
                () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"dash_interval\":0}")));
        assertThrows(IllegalArgumentException.class,
                () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"dash_power\":-1}")));
        assertThrows(IllegalArgumentException.class,
                () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"tension_gain\":0}")));
    }

    @Test
    void rejectsWrongTypes() {
        assertThrows(IllegalArgumentException.class,
                () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"stamina\":\"lots\"}")));
        assertThrows(IllegalArgumentException.class,
                () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"dash_interval\":40.5}")));
        assertThrows(IllegalArgumentException.class, () -> FishProfileParser.parse(json("{\"item\":5}")));
    }
}
