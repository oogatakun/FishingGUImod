package com.example.fishinggui.net;

import com.example.fishinggui.block.TakoyakiService;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Client to server: use one tool on one well of the takoyaki pan. The server validates everything. */
public record TakoyakiActionPacket(BlockPos pos, int tool, int well) {
    public static void encode(TakoyakiActionPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeByte(msg.tool);
        buf.writeByte(msg.well);
    }

    public static TakoyakiActionPacket decode(FriendlyByteBuf buf) {
        return new TakoyakiActionPacket(buf.readBlockPos(), buf.readByte(), buf.readByte());
    }

    public static void handle(TakoyakiActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null) {
            TakoyakiService.apply(sender, msg.pos, msg.tool, msg.well);
        }
        ctx.get().setPacketHandled(true);
    }
}
