package com.example.fishinggui.client;

import com.example.fishinggui.net.SkillSyncPacket;
import java.util.List;
import net.minecraft.client.Minecraft;

/** Client-side copy of the player's points and skills. */
public final class ClientSkills {
    private static volatile int points;
    private static volatile List<SkillSyncPacket.Entry> skills = List.of();

    private ClientSkills() {
    }

    public static void apply(int newPoints, List<SkillSyncPacket.Entry> newSkills) {
        points = newPoints;
        skills = List.copyOf(newSkills);
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.screen instanceof SkillScreen screen) {
                screen.refresh();
            }
        });
    }

    public static int points() {
        return points;
    }

    public static List<SkillSyncPacket.Entry> skills() {
        return skills;
    }
}
