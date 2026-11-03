package com.example.fishinggui.net;

import com.example.fishinggui.skill.SkillService;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Client to server: buy one level of a skill. The server validates everything. */
public record SkillBuyPacket(String id) {
    public static void encode(SkillBuyPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.id, 64);
    }

    public static SkillBuyPacket decode(FriendlyByteBuf buf) {
        return new SkillBuyPacket(buf.readUtf(64));
    }

    public static void handle(SkillBuyPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null) {
            SkillService.buy(sender, msg.id);
        }
    }
}
