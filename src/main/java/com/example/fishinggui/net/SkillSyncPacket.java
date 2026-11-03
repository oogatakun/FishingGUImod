package com.example.fishinggui.net;

import com.example.fishinggui.client.ClientSkills;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server to client: points, skill definitions and the player's current skill levels. */
public record SkillSyncPacket(int points, List<Entry> skills) {
    public record Entry(String id, int maxLevel, List<Integer> cost, int level) {
    }

    public static void encode(SkillSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.points);
        buf.writeVarInt(msg.skills.size());
        for (Entry e : msg.skills) {
            buf.writeUtf(e.id, 64);
            buf.writeVarInt(e.maxLevel);
            buf.writeVarInt(e.cost.size());
            for (int c : e.cost) {
                buf.writeVarInt(c);
            }
            buf.writeVarInt(e.level);
        }
    }

    public static SkillSyncPacket decode(FriendlyByteBuf buf) {
        int points = buf.readVarInt();
        int n = buf.readVarInt();
        List<Entry> skills = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String id = buf.readUtf(64);
            int max = buf.readVarInt();
            int costs = buf.readVarInt();
            List<Integer> cost = new ArrayList<>();
            for (int j = 0; j < costs; j++) {
                cost.add(buf.readVarInt());
            }
            skills.add(new Entry(id, max, cost, buf.readVarInt()));
        }
        return new SkillSyncPacket(points, skills);
    }

    public static void handle(SkillSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSkills.apply(msg.points, msg.skills));
    }
}
