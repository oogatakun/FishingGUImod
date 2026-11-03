package com.example.fishinggui.level;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/** Reads and writes fishing progress in the player's persistent data (kept across death by Forge). */
public final class FishingData {
    private static final String KEY = "fishinggui";

    private FishingData() {
    }

    public static FishingProgress get(Player player) {
        return read(player.getPersistentData());
    }

    public static void set(Player player, FishingProgress progress) {
        write(player.getPersistentData(), progress);
    }

    /**
     * The mod's tag inside {@code persistentData}, created and attached if missing. Shared by every writer so
     * that writing one field (level/xp, points, skills) never discards the others.
     */
    public static CompoundTag modTag(CompoundTag persistentData) {
        CompoundTag root = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag tag = root.getCompound(KEY);
        root.put(KEY, tag);
        persistentData.put(Player.PERSISTED_NBT_TAG, root);
        return tag;
    }

    /** The mod's tag if present, otherwise a detached empty tag. Never modifies {@code persistentData}. */
    public static CompoundTag modTagIfPresent(CompoundTag persistentData) {
        return persistentData.getCompound(Player.PERSISTED_NBT_TAG).getCompound(KEY);
    }

    public static FishingProgress read(CompoundTag persistentData) {
        CompoundTag tag = persistentData.getCompound(Player.PERSISTED_NBT_TAG).getCompound(KEY);
        if (!tag.contains("level")) {
            return FishingProgress.START;
        }
        return new FishingProgress(tag.getInt("level"), tag.getInt("xp"));
    }

    public static void write(CompoundTag persistentData, FishingProgress progress) {
        CompoundTag tag = modTag(persistentData);
        tag.putInt("level", progress.level());
        tag.putInt("xp", progress.xp());
    }
}
