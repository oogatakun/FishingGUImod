package com.example.fishinggui.hook;

import com.example.fishinggui.FishingGuiMod;
import com.example.fishinggui.data.FishProfileLoader;
import com.example.fishinggui.fight.FightManager;
import com.example.fishinggui.fight.FishInfo;
import com.example.fishinggui.fight.FishProfile;
import com.example.fishinggui.level.FishingData;
import com.example.fishinggui.skill.SkillData;
import com.example.fishinggui.skill.SkillEffects;
import com.example.fishinggui.skill.SkillState;
import java.util.List;
import java.util.Random;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
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
        int fishingLevel = FishingData.get(player).level();
        ItemStack offhand = event.getHand() == InteractionHand.MAIN_HAND ? player.getOffhandItem() : ItemStack.EMPTY;
        int baitLevel = FishProfileLoader.baitLevelOf(offhand);
        Item bait = null;
        if (baitLevel > 0) {
            if (baitLevel <= fishingLevel) {
                bait = offhand.getItem();
            } else {
                // Chat as well as the action bar: the fight screen can cover the action bar.
                player.displayClientMessage(Component.translatable("gui.fishinggui.bait.locked", baitLevel), false);
                player.displayClientMessage(Component.translatable("gui.fishinggui.bait.locked", baitLevel), true);
            }
        }
        List<ItemStack> loot = CatchResolver.roll(player, event.getItemStack(), bait == null ? 0 : baitLevel, bait);
        FishInfo info = FishProfileLoader.infoFor(loot);
        float weight = info.rollWeight(new Random());
        SkillState skills = SkillData.get(player);
        FishProfile profile = SkillEffects.applyToProfile(
                info.scaleForWeight(FishProfileLoader.profileFor(loot), weight), fishingLevel,
                skills.levelOf(SkillEffects.DASH_EASE), skills.levelOf(SkillEffects.DASH_DELAY),
                skills.levelOf(SkillEffects.REEL_SPEED));
        FightManager.start(player, hook, event.getHand(), profile, bait, loot, info, weight);
    }

    private static void consume(PlayerInteractEvent.RightClickItem event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
