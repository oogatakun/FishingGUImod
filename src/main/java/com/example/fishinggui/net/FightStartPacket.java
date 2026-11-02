package com.example.fishinggui.net;

import com.example.fishinggui.client.ClientFightHooks;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server to client: a fight began, open the GUI. */
public record FightStartPacket(float maxStamina) {
    public static void encode(FightStartPacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.maxStamina);
    }

    public static FightStartPacket decode(FriendlyByteBuf buf) {
        return new FightStartPacket(buf.readFloat());
    }

    public static void handle(FightStartPacket msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientFightHooks.onStart(msg.maxStamina));
    }
}
