package com.example.fishinggui.net;

import com.example.fishinggui.hook.PendingCatchManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Client to server: Keep (true) or Release (false) the pending catch. */
public record CatchChoicePacket(boolean keep) {
    public static void encode(CatchChoicePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.keep);
    }

    public static CatchChoicePacket decode(FriendlyByteBuf buf) {
        return new CatchChoicePacket(buf.readBoolean());
    }

    public static void handle(CatchChoicePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null) {
            PendingCatchManager.choose(sender, msg.keep);
        }
    }
}
