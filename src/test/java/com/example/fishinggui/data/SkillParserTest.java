package com.example.fishinggui.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.fishinggui.skill.SkillDef;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class SkillParserTest {
    private static JsonObject json(String s) {
        return JsonParser.parseString(s).getAsJsonObject();
    }

    private static void bad(String s) {
        assertThrows(IllegalArgumentException.class, () -> SkillParser.parse("x", json(s)));
    }

    @Test
    void parsesValid() {
        SkillDef d = SkillParser.parse("x", json("{\"max_level\":3,\"cost\":[1,2,3]}"));
        assertEquals("x", d.id());
        assertEquals(3, d.maxLevel());
        assertEquals(2, d.nextCost(1));
    }

    @Test
    void rejectsLengthMismatch() {
        bad("{\"max_level\":3,\"cost\":[1,2]}");
    }

    @Test
    void rejectsNonPositiveCost() {
        bad("{\"max_level\":2,\"cost\":[1,0]}");
        bad("{\"max_level\":2,\"cost\":[1,-3]}");
    }

    @Test
    void rejectsBadMaxLevel() {
        bad("{\"cost\":[1]}");
        bad("{\"max_level\":0,\"cost\":[]}");
        bad("{\"max_level\":-1,\"cost\":[]}");
    }

    @Test
    void rejectsMissingCost() {
        bad("{\"max_level\":2}");
    }
}
