package com.example.fishinggui.net;

import com.example.fishinggui.client.ClientFightHooks;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server to client: a fight succeeded, open the catch screen. */
public record CatchResultPacket(ItemStack stack, byte rarity, float weight, int xp) {
    public static void encode(CatchResultPacket msg, FriendlyByteBuf buf) {
        buf.writeItem(msg.stack);
        buf.writeByte(msg.rarity);
        buf.writeFloat(msg.weight);
        buf.writeVarInt(msg.xp);
    }

    public static CatchResultPacket decode(FriendlyByteBuf buf) {
        return new CatchResultPacket(buf.readItem(), buf.readByte(), buf.readFloat(), buf.readVarInt());
    }

    public static void handle(CatchResultPacket msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientFightHooks.onCatchResult(msg.stack, msg.rarity, msg.weight, msg.xp));
    }
}
