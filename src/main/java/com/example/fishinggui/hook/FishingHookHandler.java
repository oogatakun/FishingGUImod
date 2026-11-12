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
        if (tackle && hook == null && !setup.complete()) {
            // no line, rod or hook: nothing to cast with
            player.displayClientMessage(Component.translatable("gui.fishinggui.rod.incomplete"), true);
            consume(event);
            return;
        }
        if (tackle && hook == null && player.isShiftKeyDown() && event.getHand() == InteractionHand.MAIN_HAND
                && FishProfileLoader.baitLevelOf(player.getOffhandItem()) > 0
                && player.getOffhandItem().getItem() != TackleRodItem.baitOf(rodStack)) {
            // an uncast rod, sneaking, a fish in the off hand: fit it to the hook
            attachBait(player, rodStack);
            consume(event);
            return;
        }
        if (tackle && hook == null && player.isShiftKeyDown() && event.getHand() == InteractionHand.MAIN_HAND
                && (player.getOffhandItem().isEmpty() || player.getOffhandItem().getItem() == TackleRodItem.baitOf(rodStack))
                && TackleRodItem.baitOf(rodStack) != null) {
            // an uncast rod, sneaking, and the off hand empty or holding the same fish as the bait: take it off
            detachBait(player, rodStack);
            consume(event);
            return;
        }
        Item bait = tackle ? TackleRodItem.baitOf(rodStack) : null;
        int baitLevel = bait == null ? 0 : FishProfileLoader.baitLevelOf(new ItemStack(bait));
        if (hook == null) {
            // Casting: the bait fitted to the hook must be one the player can use.
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

    private static void detachBait(ServerPlayer player, ItemStack rod) {
        ItemStack back = new ItemStack(TackleRodItem.baitOf(rod));
        TackleRodItem.setBait(rod, null);
        if (!player.getInventory().add(back)) {
            player.drop(back, false);
        }
        player.displayClientMessage(Component.translatable("gui.fishinggui.bait.detached", back.getHoverName()), true);
    }

    private static void attachBait(ServerPlayer player, ItemStack rod) {
        ItemStack offhand = player.getOffhandItem();
        Item old = TackleRodItem.baitOf(rod);
        Item bait = offhand.getItem();
        TackleRodItem.setBait(rod, bait);
        if (!player.getAbilities().instabuild) {
            offhand.shrink(1);
        }
        if (old != null) {
            ItemStack back = new ItemStack(old);
            if (!player.getInventory().add(back)) {
                player.drop(back, false);
            }
        }
        player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.FISHING_BOBBER_RETRIEVE,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 1.2f);
        player.displayClientMessage(Component.translatable("gui.fishinggui.bait.attached", bait.getDescription()),
                true);
    }

    /** True if the bait is usable; otherwise tells the player why (on the action bar only). */
    private static boolean baitUsable(ServerPlayer player, int baitLevel, int fishingLevel, int rodCap) {
        if (baitLevel > 0 && baitLevel <= fishingLevel && baitLevel <= rodCap) {
            return true;
        }
        Component msg = baitLevel > fishingLevel || baitLevel == 0 ? null : Component.translatable(
                "gui.fishinggui.bait.rod_cap", rodCap);
        if (msg != null) {
            player.displayClientMessage(msg, true);
            return false;
        }
        msg = baitLevel > 0 ? Component.translatable("gui.fishinggui.bait.locked", baitLevel)
                : Component.translatable("gui.fishinggui.bait.required");
        player.displayClientMessage(msg, true);
        return false;
    }

    private static void consume(PlayerInteractEvent.RightClickItem event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
