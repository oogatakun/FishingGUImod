package com.example.fishinggui.net;

import com.example.fishinggui.client.ClientFightHooks;
import com.example.fishinggui.fight.FightSession;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server to client: per-tick fight state. A phase other than ACTIVE closes the GUI. */
public record FightStatePacket(float tension, float stamina, boolean telegraph, boolean dashing,
                               FightSession.Phase phase) {
    public static void encode(FightStatePacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.tension);
        buf.writeFloat(msg.stamina);
        buf.writeBoolean(msg.telegraph);
        buf.writeBoolean(msg.dashing);
        buf.writeEnum(msg.phase);
    }

    public static FightStatePacket decode(FriendlyByteBuf buf) {
        return new FightStatePacket(buf.readFloat(), buf.readFloat(), buf.readBoolean(), buf.readBoolean(),
                buf.readEnum(FightSession.Phase.class));
    }

    public static void handle(FightStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientFightHooks.onState(
                msg.tension, msg.stamina, msg.telegraph, msg.dashing, msg.phase));
    }
}
