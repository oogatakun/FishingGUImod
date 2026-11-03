package com.example.fishinggui.skill;

import com.example.fishinggui.FishingGuiMod;
import com.example.fishinggui.data.SkillLoader;
import com.example.fishinggui.net.ModNetwork;
import com.example.fishinggui.net.SkillSyncPacket;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/** Applies point and skill changes on the server and keeps the client in sync. */
@Mod.EventBusSubscriber(modid = FishingGuiMod.MOD_ID)
public final class SkillService {
    private SkillService() {
    }

    /** Adds (or, if negative, removes) points; the result never goes below 0. */
    public static void addPoints(ServerPlayer player, int amount) {
        SkillData.set(player, SkillData.get(player).addPoints(amount));
        sync(player);
    }

    public static void setPoints(ServerPlayer player, int points) {
        SkillData.set(player, SkillData.get(player).withPoints(points));
        sync(player);
    }

    public static void buy(ServerPlayer player, String skillId) {
        SkillDef def = SkillLoader.skills().get(skillId);
        if (def == null) {
            return;
        }
        SkillState state = SkillData.get(player);
        if (!state.canBuy(def)) {
            return;
        }
        SkillData.set(player, state.buy(def));
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        SkillState state = SkillData.get(player);
        List<SkillSyncPacket.Entry> entries = new ArrayList<>();
        for (SkillDef def : SkillLoader.skills().values()) {
            entries.add(new SkillSyncPacket.Entry(def.id(), def.maxLevel(), def.cost(), state.levelOf(def.id())));
        }
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SkillSyncPacket(state.points(), entries));
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
