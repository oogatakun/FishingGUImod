package com.example.fishinggui.net;

import com.example.fishinggui.hook.PendingCatchManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Client to server: Keep, Release or Convert-to-points for the pending catch. */
public record CatchChoicePacket(byte choice) {
    public static final byte KEEP = 0;
    public static final byte RELEASE = 1;
    public static final byte CONVERT = 2;

    public static void encode(CatchChoicePacket msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.choice);
    }

    public static CatchChoicePacket decode(FriendlyByteBuf buf) {
        return new CatchChoicePacket(buf.readByte());
    }

    public static void handle(CatchChoicePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null) {
            int c = msg.choice == RELEASE || msg.choice == CONVERT ? msg.choice : KEEP;
            PendingCatchManager.choose(sender, c);
        }
    }
}
