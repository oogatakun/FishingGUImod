package com.example.fishinggui.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class FishLevelsTest {
    @Test
    void tableMatchesProfileJson() throws IOException {
        Path dir = Path.of("src/main/resources/data/fishinggui/fishinggui/fish_profiles");
        int checked = 0;
        List<Path> files;
        try (Stream<Path> s = Files.list(dir)) {
            files = s.filter(p -> p.toString().endsWith(".json")).toList();
        }
        for (Path file : files) {
            JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            if (!json.has("bait_level")) {
                continue;
            }
            String item = json.get("item").getAsString();
            assertEquals(json.get("bait_level").getAsInt(), FishLevels.BY_ID.get(item), item);
            checked++;
        }
        assertEquals(FishLevels.BY_ID.size(), checked);
        assertTrue(checked > 50);
    }
}
