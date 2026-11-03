package com.example.fishinggui.level;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/** Reads and writes fishing progress in the player's persistent data (kept across death by Forge). */
public final class FishingData {
    private static final String KEY = "fishinggui";

    private FishingData() {
    }

    public static FishingProgress get(Player player) {
        CompoundTag root = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag tag = root.getCompound(KEY);
        if (!tag.contains("level")) {
            return FishingProgress.START;
        }
        return new FishingProgress(tag.getInt("level"), tag.getInt("xp"));
    }

    public static void set(Player player, FishingProgress progress) {
        CompoundTag persistent = player.getPersistentData();
        CompoundTag root = persistent.getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag tag = new CompoundTag();
        tag.putInt("level", progress.level());
        tag.putInt("xp", progress.xp());
        root.put(KEY, tag);
        persistent.put(Player.PERSISTED_NBT_TAG, root);
    }
}
