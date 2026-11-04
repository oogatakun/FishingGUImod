package com.example.fishinggui.net;

import com.example.fishinggui.skill.SkillService;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Client to server: reset one skill using a reset drop. The server validates everything. */
public record SkillResetPacket(String skillId) {
    public static void encode(SkillResetPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.skillId(), 64);
    }

    public static SkillResetPacket decode(FriendlyByteBuf buf) {
        return new SkillResetPacket(buf.readUtf(64));
    }

    public static void handle(SkillResetPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null) {
            SkillService.reset(sender, msg.skillId());
        }
    }
}
