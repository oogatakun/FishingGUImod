package com.example.fishinggui.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void foodOverrideAbsentByDefault() {
        FishProfileParser.Entry e = FishProfileParser.parse(json("{\"item\":\"minecraft:cod\",\"stamina\":70}"));
        assertTrue(e.info().foodOverride().isEmpty());
        assertNull(e.info().food());
    }

    @Test
    void parsesFoodOverride() {
        FishProfileParser.Entry e = FishProfileParser.parse(json("{\"item\":\"a:b\",\"stamina\":70,"
                + "\"food\":{\"nutrition\":6,\"saturation\":0.8,\"heal\":3}}"));
        assertEquals(new com.example.fishinggui.fight.FoodOverride(6, 0.8f, 3), e.info().foodOverride().get());
    }

    @Test
    void rejectsInvalidFoodOverride() {
        for (String food : new String[] { "{\"nutrition\":-1,\"saturation\":0.5,\"heal\":0}",
                "{\"nutrition\":2.5,\"saturation\":0.5,\"heal\":0}",
                "{\"nutrition\":2,\"saturation\":-0.5,\"heal\":0}",
                "{\"nutrition\":2,\"saturation\":0.5,\"heal\":-1}",
                "{\"nutrition\":2,\"saturation\":0.5}", "5" }) {
            assertThrows(IllegalArgumentException.class,
                    () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"food\":" + food + "}")), food);
        }
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

    @Test
    void parsesFishInfoFields() {
        FishProfileParser.Entry e = FishProfileParser.parse(json(
                "{\"item\":\"a:b\",\"rarity\":\"epic\",\"weight_min\":2,\"weight_max\":5,\"xp\":80}"));
        assertEquals(new com.example.fishinggui.fight.FishInfo(
                com.example.fishinggui.fight.Rarity.EPIC, 2f, 5f, 80), e.info());
    }

    @Test
    void fishInfoDefaultsWhenOmitted() {
        assertEquals(com.example.fishinggui.fight.FishInfo.DEFAULT,
                FishProfileParser.parse(json("{\"item\":\"a:b\"}")).info());
    }

    @Test
    void rejectsInvalidFishInfo() {
        assertThrows(IllegalArgumentException.class,
                () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"rarity\":\"mythic\"}")));
        assertThrows(IllegalArgumentException.class,
                () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"weight_min\":0}")));
        assertThrows(IllegalArgumentException.class,
                () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"weight_min\":2,\"weight_max\":1}")));
        assertThrows(IllegalArgumentException.class,
                () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"xp\":-1}")));
        assertThrows(IllegalArgumentException.class,
                () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"xp\":1.5}")));
        assertThrows(IllegalArgumentException.class,
                () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"rarity\":3}")));
    }
}
