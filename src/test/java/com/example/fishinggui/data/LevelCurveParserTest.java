package com.example.fishinggui.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.fishinggui.level.LevelCurve;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class LevelCurveParserTest {
    private static JsonObject json(String s) {
        return JsonParser.parseString(s).getAsJsonObject();
    }

    private static String array(int n, String element) {
        StringBuilder sb = new StringBuilder("{\"xp_to_next\":[");
        for (int i = 0; i < n; i++) {
            sb.append(i == 0 ? "" : ",").append(element);
        }
        return sb.append("]}").toString();
    }

    @Test
    void parsesNineteenValues() {
        LevelCurve c = LevelCurveParser.parse(json(array(19, "7")));
        assertEquals(7, c.xpToNext(1));
        assertEquals(7, c.xpToNext(19));
        assertEquals(0, c.xpToNext(20));
    }

    @Test
    void rejectsWrongLength() {
        assertThrows(IllegalArgumentException.class, () -> LevelCurveParser.parse(json(array(18, "7"))));
        assertThrows(IllegalArgumentException.class, () -> LevelCurveParser.parse(json(array(20, "7"))));
    }

    @Test
    void rejectsZeroNegativeAndFractions() {
        assertThrows(IllegalArgumentException.class, () -> LevelCurveParser.parse(json(array(19, "0"))));
        assertThrows(IllegalArgumentException.class, () -> LevelCurveParser.parse(json(array(19, "-4"))));
        assertThrows(IllegalArgumentException.class, () -> LevelCurveParser.parse(json(array(19, "2.5"))));
    }

    @Test
    void rejectsMissingOrNonArray() {
        assertThrows(IllegalArgumentException.class, () -> LevelCurveParser.parse(json("{}")));
        assertThrows(IllegalArgumentException.class,
                () -> LevelCurveParser.parse(json("{\"xp_to_next\": 5}")));
    }
}
