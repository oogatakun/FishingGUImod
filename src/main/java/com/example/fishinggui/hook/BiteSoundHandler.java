package com.example.fishinggui.hook;

import com.example.fishinggui.FishingGuiMod;
import com.example.fishinggui.fight.FightManager;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Plays an XP-orb chime to the player when a fish takes the bait (before they click). */
@Mod.EventBusSubscriber(modid = FishingGuiMod.MOD_ID)
public final class BiteSoundHandler {
    private static final Set<UUID> BITING = new HashSet<>();

    private BiteSoundHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        FishingHook hook = player.fishing;
        boolean biting = hook != null && hook.nibble > 0;
        boolean wasBiting = biting ? !BITING.add(player.getUUID()) : BITING.remove(player.getUUID());
        if (biting && !wasBiting && !FightManager.isFighting(player)) {
            player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.9f, 1.0f);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        BITING.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        BITING.clear();
    }
}
