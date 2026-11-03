package com.example.fishinggui.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.fishinggui.level.FishingData;
import com.example.fishinggui.level.FishingProgress;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class SkillDataTest {
    @Test
    void pointsAndSkillsRoundTrip() {
        CompoundTag persistent = new CompoundTag();
        SkillData.write(persistent, new SkillState(7, Map.of("bite_speed", 3)));
        SkillState back = SkillData.read(persistent);
        assertEquals(7, back.points());
        assertEquals(3, back.levelOf("bite_speed"));
    }

    @Test
    void writingPointsKeepsLevelAndXp() {
        CompoundTag persistent = new CompoundTag();
        FishingData.write(persistent, new FishingProgress(5, 12));
        SkillData.write(persistent, new SkillState(4, Map.of("bite_speed", 1)));
        FishingProgress p = FishingData.read(persistent);
        assertEquals(5, p.level());
        assertEquals(12, p.xp());
        assertEquals(4, SkillData.read(persistent).points());
    }

    @Test
    void writingLevelKeepsPointsAndSkills() {
        CompoundTag persistent = new CompoundTag();
        SkillData.write(persistent, new SkillState(4, Map.of("bite_speed", 2)));
        FishingData.write(persistent, new FishingProgress(9, 3));
        SkillState s = SkillData.read(persistent);
        assertEquals(4, s.points());
        assertEquals(2, s.levelOf("bite_speed"));
        assertEquals(9, FishingData.read(persistent).level());
    }

    @Test
    void emptyDataReadsAsStart() {
        CompoundTag persistent = new CompoundTag();
        assertEquals(SkillState.START, SkillData.read(persistent));
        assertEquals(FishingProgress.START, FishingData.read(persistent));
    }
}
