package com.example.fishinggui.fight;

import com.example.fishinggui.FishingGuiMod;
import com.example.fishinggui.hook.CatchResolver;
import com.example.fishinggui.item.ModItems;
import com.example.fishinggui.level.FishingService;
import com.example.fishinggui.net.FightStartPacket;
import com.example.fishinggui.net.FightStatePacket;
import com.example.fishinggui.net.ModNetwork;
import com.example.fishinggui.net.ReelInputPacket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/** Owns one {@link FightSession} per fighting player and advances them on the server thread. */
@Mod.EventBusSubscriber(modid = FishingGuiMod.MOD_ID)
public final class FightManager {
    private record ActiveFight(FightSession session, FishingHook hook, InteractionHand hand, BaitType bait,
            List<ItemStack> loot, FishInfo info, float weight) {
    }

    private static final Map<UUID, ActiveFight> FIGHTS = new HashMap<>();

    private FightManager() {
    }

    public static boolean isFighting(Player player) {
        return FIGHTS.containsKey(player.getUUID());
    }

    public static void start(ServerPlayer player, FishingHook hook, InteractionHand hand, FishProfile profile, BaitType bait,
            List<ItemStack> loot, FishInfo info, float weight) {
        FIGHTS.put(player.getUUID(), new ActiveFight(new FightSession(profile, new Random()), hook, hand, bait, loot,
                info, weight));
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new FightStartPacket(profile.stamina()));
        FishingService.sync(player);
    }

    public static void handleInput(ServerPlayer player, byte action) {
        ActiveFight fight = FIGHTS.get(player.getUUID());
        if (fight == null) {
            return;
        }
        switch (action) {
            case ReelInputPacket.REEL -> fight.session().setReeling(true);
            case ReelInputPacket.RELEASE -> fight.session().setReeling(false);
            case ReelInputPacket.ABORT -> end(player, fight, FightSession.Phase.FAILED, false);
            default -> {
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        ActiveFight fight = FIGHTS.get(player.getUUID());
        if (fight == null) {
            return;
        }
        if (!stillValid(player, fight)) {
            end(player, fight, FightSession.Phase.FAILED, false);
            return;
        }
        // Pin the bite state so vanilla does not start a new lure/bite cycle during a fight.
        fight.hook().nibble = Math.max(fight.hook().nibble, 2);
        FightSession session = fight.session();
        session.tick();
        if (session.phase() != FightSession.Phase.ACTIVE) {
            end(player, fight, session.phase(), session.lineBroke());
            return;
        }
        sendState(player, session, FightSession.Phase.ACTIVE);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        FIGHTS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        FIGHTS.clear();
    }

    private static boolean stillValid(ServerPlayer player, ActiveFight fight) {
        return player.isAlive()
                && !fight.hook().isRemoved()
                && player.fishing == fight.hook()
                && player.getItemInHand(fight.hand()).getItem() instanceof FishingRodItem;
    }

    private static void end(ServerPlayer player, ActiveFight fight, FightSession.Phase phase, boolean lineBroke) {
        FIGHTS.remove(player.getUUID());
        sendState(player, fight.session(), phase);
        if (phase == FightSession.Phase.SUCCESS) {
            CatchResolver.success(player, fight.hook(), fight.hand(), fight.loot(), fight.info(),
                    fight.weight());
        } else {
            CatchResolver.failure(player, fight.hook(), fight.hand(), lineBroke);
        }
        consumeBait(player, fight.bait());
    }

    private static void consumeBait(ServerPlayer player, BaitType bait) {
        if (bait == BaitType.NONE || player.getAbilities().instabuild) {
            return;
        }
        ItemStack offhand = player.getOffhandItem();
        if (ModItems.typeOf(offhand) == bait) {
            offhand.shrink(1);
        }
    }

    private static void sendState(ServerPlayer player, FightSession s, FightSession.Phase phase) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new FightStatePacket(s.tension(), s.stamina(), s.telegraphing(), s.dashing(), phase));
    }
}
