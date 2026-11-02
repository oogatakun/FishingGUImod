package com.example.fishinggui.hook;

import com.example.fishinggui.FishingGuiMod;
import com.example.fishinggui.data.FishProfileLoader;
import com.example.fishinggui.fight.BaitEffect;
import com.example.fishinggui.fight.BaitType;
import com.example.fishinggui.fight.FightManager;
import com.example.fishinggui.fight.FishInfo;
import com.example.fishinggui.fight.FishProfile;
import com.example.fishinggui.item.ModItems;
import java.util.List;
import java.util.Random;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Turns the right-click that would instantly reel in a biting fish into the start of a tension fight. */
@Mod.EventBusSubscriber(modid = FishingGuiMod.MOD_ID)
public final class FishingHookHandler {
    private FishingHookHandler() {
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(event.getItemStack().getItem() instanceof FishingRodItem)) {
            return;
        }
        if (FightManager.isFighting(player)) {
            consume(event);
            return;
        }
        FishingHook hook = player.fishing;
        if (hook == null || hook.nibble <= 0) {
            return;
        }
        consume(event);
        BaitType bait = event.getHand() == InteractionHand.MAIN_HAND
                ? ModItems.typeOf(player.getOffhandItem())
                : BaitType.NONE;
        List<ItemStack> loot = CatchResolver.roll(player, hook, event.getItemStack(), BaitEffect.luckBonus(bait));
        FishInfo info = FishProfileLoader.infoFor(loot);
        float weight = info.rollWeight(new Random());
        FishProfile profile = BaitEffect.apply(bait,
                info.scaleForWeight(FishProfileLoader.profileFor(loot), weight));
        FightManager.start(player, hook, event.getHand(), profile, bait, loot, info, weight);
    }

    private static void consume(PlayerInteractEvent.RightClickItem event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
