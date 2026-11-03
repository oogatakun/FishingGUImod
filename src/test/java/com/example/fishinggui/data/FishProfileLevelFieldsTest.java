package com.example.fishinggui.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class FishProfileLevelFieldsTest {
    private static JsonObject json(String s) {
        return JsonParser.parseString(s).getAsJsonObject();
    }

    @Test
    void defaultsWhenAbsent() {
        FishProfileParser.Entry e = FishProfileParser.parse(json("{\"item\":\"minecraft:cod\"}"));
        assertEquals(0, e.info().catchLevel());
        assertEquals(10, e.info().catchWeight());
        assertEquals(0, e.info().baitLevel());
    }

    @Test
    void parsesAllThree() {
        FishProfileParser.Entry e = FishProfileParser.parse(
                json("{\"item\":\"a:b\",\"level\":7,\"catch_weight\":100,\"bait_level\":4}"));
        assertEquals(7, e.info().catchLevel());
        assertEquals(100, e.info().catchWeight());
        assertEquals(4, e.info().baitLevel());
    }

    @Test
    void baitOnlyEntryIsValid() {
        FishProfileParser.Entry e = FishProfileParser.parse(json("{\"item\":\"fishinggui:worm\",\"bait_level\":3}"));
        assertEquals(3, e.info().baitLevel());
        assertEquals(0, e.info().catchLevel());
    }

    @Test
    void rejectsOutOfRangeAndFractions() {
        assertThrows(IllegalArgumentException.class,
                () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"level\":0}")));
        assertThrows(IllegalArgumentException.class,
                () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"level\":21}")));
        assertThrows(IllegalArgumentException.class,
                () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"bait_level\":0}")));
        assertThrows(IllegalArgumentException.class,
                () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"bait_level\":2.5}")));
        assertThrows(IllegalArgumentException.class,
                () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"catch_weight\":0}")));
    }
}
