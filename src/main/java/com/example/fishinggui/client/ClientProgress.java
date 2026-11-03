package com.example.fishinggui.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Latest fishing progress known to the client. */
public final class ClientProgress {
    private static int level = 1;
    private static int xp = 0;
    private static int xpToNext = 50;

    private ClientProgress() {
    }

    public static int level() {
        return level;
    }

    public static int xp() {
        return xp;
    }

    public static int xpToNext() {
        return xpToNext;
    }

    public static void apply(int newLevel, int newXp, int newXpToNext, int gained, boolean leveledUp) {
        level = newLevel;
        xp = newXp;
        xpToNext = newXpToNext;
        var player = Minecraft.getInstance().player;
        if (gained > 0 && player != null) {
            Component msg = xpToNext == 0
                    ? Component.translatable("gui.fishinggui.xp.gained.max", gained, level)
                    : Component.translatable("gui.fishinggui.xp.gained", gained, level, xp, xpToNext);
            player.sendSystemMessage(msg);
        }
        if (leveledUp) {
            LevelUpOverlay.start(level);
        }
    }
}
