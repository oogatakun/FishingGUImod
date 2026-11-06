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
import com.example.fishinggui.item.TackleRodItem;
import com.example.fishinggui.tackle.RodSetup;
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
        ItemStack rodStack = event.getItemStack();
        // only a rod built at a tackle bench fishes properly; the vanilla rod brings up junk
        boolean tackle = rodStack.getItem() instanceof TackleRodItem;
        RodSetup setup = TackleRodItem.setupOf(rodStack);
        FishingHook hook = player.fishing;
        int fishingLevel = FishingData.get(player).level();
        ItemStack offhand = event.getHand() == InteractionHand.MAIN_HAND ? player.getOffhandItem() : ItemStack.EMPTY;
        int baitLevel = tackle ? FishProfileLoader.baitLevelOf(offhand) : 0;
        if (hook == null) {
            // Casting: a bait the player can use must be held in the off hand.
            if (tackle && !baitUsable(player, baitLevel, fishingLevel, setup.levelCap())) {
                consume(event);
            }
            return;
        }
        if (hook.nibble <= 0) {
            return;
        }
        consume(event);
        if (tackle && !baitUsable(player, baitLevel, fishingLevel, setup.levelCap())) {
            hook.discard();
            return;
        }
        Item bait = tackle ? offhand.getItem() : null;
        List<ItemStack> loot = CatchResolver.roll(player, event.getItemStack(), baitLevel, bait);
        FishInfo info = FishProfileLoader.infoFor(loot);
        float weight = info.rollWeight(new Random());
        SkillState skills = SkillData.get(player);
        FishProfile profile = SkillEffects.applyToProfile(
                info.scaleForWeight(FishProfileLoader.profileFor(loot), weight), fishingLevel,
                skills.levelOf(SkillEffects.DASH_EASE), skills.levelOf(SkillEffects.DASH_DELAY),
                skills.levelOf(SkillEffects.REEL_SPEED));
        if (tackle) {
            profile = setup.apply(profile);
        }
        FightManager.start(player, hook, event.getHand(), profile, bait, loot, info, weight);
    }

    /** True if the bait is usable; otherwise tells the player why (chat as well as the action bar, since the GUI can cover it). */
    private static boolean baitUsable(ServerPlayer player, int baitLevel, int fishingLevel, int rodCap) {
        if (baitLevel > 0 && baitLevel <= fishingLevel && baitLevel <= rodCap) {
            return true;
        }
        Component msg = baitLevel > fishingLevel || baitLevel == 0 ? null : Component.translatable(
                "gui.fishinggui.bait.rod_cap", rodCap);
        if (msg != null) {
            player.displayClientMessage(msg, false);
            player.displayClientMessage(msg, true);
            return false;
        }
        msg = baitLevel > 0 ? Component.translatable("gui.fishinggui.bait.locked", baitLevel)
                : Component.translatable("gui.fishinggui.bait.required");
        player.displayClientMessage(msg, false);
        player.displayClientMessage(msg, true);
        return false;
    }

    private static void consume(PlayerInteractEvent.RightClickItem event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
