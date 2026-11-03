package com.example.fishinggui.net;

import com.example.fishinggui.client.ClientProgress;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server to client: the player's current fishing progress, plus what just changed. */
public record FishingProgressPacket(int level, int xp, int xpToNext, int gained, boolean leveledUp) {
    public static void encode(FishingProgressPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.level);
        buf.writeVarInt(msg.xp);
        buf.writeVarInt(msg.xpToNext);
        buf.writeVarInt(msg.gained);
        buf.writeBoolean(msg.leveledUp);
    }

    public static FishingProgressPacket decode(FriendlyByteBuf buf) {
        return new FishingProgressPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readBoolean());
    }

    public static void handle(FishingProgressPacket msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientProgress.apply(msg.level, msg.xp, msg.xpToNext,
                msg.gained, msg.leveledUp));
    }
}
