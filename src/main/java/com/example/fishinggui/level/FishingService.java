package com.example.fishinggui.level;

import com.example.fishinggui.FishingGuiMod;
import com.example.fishinggui.data.LevelCurveLoader;
import com.example.fishinggui.net.FishingProgressPacket;
import com.example.fishinggui.net.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/** Applies fishing progress changes on the server and keeps the client in sync. */
@Mod.EventBusSubscriber(modid = FishingGuiMod.MOD_ID)
public final class FishingService {
    private FishingService() {
    }

    public static void award(ServerPlayer player, int xp) {
        FishingProgress now = FishingData.get(player);
        change(player, now.addXp(xp, LevelCurveLoader.curve()), xp);
    }

    /** Stores {@code next}; {@code gainedXp} is shown in chat when positive. */
    public static void change(ServerPlayer player, FishingProgress next, int gainedXp) {
        FishingProgress before = FishingData.get(player);
        FishingData.set(player, next);
        send(player, next, Math.max(0, gainedXp), next.level() > before.level());
    }

    public static void sync(ServerPlayer player) {
        send(player, FishingData.get(player), 0, false);
    }

    private static void send(ServerPlayer player, FishingProgress p, int gained, boolean leveledUp) {
        int needed = LevelCurveLoader.curve().xpToNext(p.level());
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new FishingProgressPacket(p.level(), p.xp(), needed, gained, leveledUp));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }
}
