package com.example.fishinggui.hook;

import com.example.fishinggui.FishingGuiMod;
import com.example.fishinggui.fight.FishInfo;
import com.example.fishinggui.net.CatchResultPacket;
import com.example.fishinggui.net.ModNetwork;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/** Holds a successful catch until the player picks Keep or Release on the catch screen. */
@Mod.EventBusSubscriber(modid = FishingGuiMod.MOD_ID)
public final class PendingCatchManager {
    private record Pending(Vec3 origin, List<ItemStack> loot) {
    }

    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private PendingCatchManager() {
    }

    public static void open(ServerPlayer player, Vec3 origin, List<ItemStack> loot, FishInfo info, float weight) {
        Pending old = PENDING.put(player.getUUID(), new Pending(origin, loot));
        if (old != null) {
            CatchResolver.deliver(player, old.origin(), old.loot(), true);
        }
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new CatchResultPacket(loot.get(0), (byte) info.rarity().ordinal(), weight, info.xp()));
    }

    /** Keep delivers the items (plus the vanilla XP orb); Release delivers nothing. */
    public static void choose(ServerPlayer player, boolean keep) {
        Pending pending = PENDING.remove(player.getUUID());
        if (pending != null && keep) {
            CatchResolver.deliver(player, pending.origin(), pending.loot(), true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            choose(player, true);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PENDING.clear();
    }
}
