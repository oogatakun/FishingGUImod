package com.example.fishinggui.skill;

import com.example.fishinggui.level.FishingData;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/** Stores points and skill levels next to level/xp in the player's persistent data. */
public final class SkillData {
    private SkillData() {
    }

    public static SkillState get(Player player) {
        return read(player.getPersistentData());
    }

    public static void set(Player player, SkillState state) {
        write(player.getPersistentData(), state);
    }

    public static SkillState read(CompoundTag persistentData) {
        CompoundTag tag = FishingData.modTagIfPresent(persistentData);
        Map<String, Integer> skills = new HashMap<>();
        CompoundTag skillTag = tag.getCompound("skills");
        for (String id : skillTag.getAllKeys()) {
            skills.put(id, skillTag.getInt(id));
        }
        return new SkillState(tag.getInt("points"), skills);
    }

    public static void write(CompoundTag persistentData, SkillState state) {
        CompoundTag tag = FishingData.modTag(persistentData);
        tag.putInt("points", state.points());
        CompoundTag skillTag = new CompoundTag();
        state.skills().forEach(skillTag::putInt);
        tag.put("skills", skillTag);
    }
}
