package com.example.fishinggui.net;

import com.example.fishinggui.fight.FightManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Client to server: reel state. Sent on change and as a periodic keep-alive while the GUI is open. */
public record ReelInputPacket(byte action) {
    public static final byte RELEASE = 0;
    public static final byte REEL = 1;
    public static final byte ABORT = 2;

    public static void encode(ReelInputPacket msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.action);
    }

    public static ReelInputPacket decode(FriendlyByteBuf buf) {
        return new ReelInputPacket(buf.readByte());
    }

    public static void handle(ReelInputPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null) {
            FightManager.handleInput(sender, msg.action);
        }
    }
}
