package com.example.fishinggui.hook;

import java.util.List;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Resolves a finished fight: vanilla loot on success, nothing on failure. */
public final class CatchResolver {
    private CatchResolver() {
    }

    public static void success(ServerPlayer player, FishingHook hook, InteractionHand hand) {
        ServerLevel level = player.serverLevel();
        ItemStack rod = player.getItemInHand(hand);
        float luck = EnchantmentHelper.getFishingLuckBonus(rod) + player.getLuck();

        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, hook.position())
                .withParameter(LootContextParams.TOOL, rod)
                .withParameter(LootContextParams.THIS_ENTITY, hook)
                .withParameter(LootContextParams.KILLER_ENTITY, player)
                .withLuck(luck)
                .create(LootContextParamSets.FISHING);
        LootTable table = level.getServer().getLootData().getLootTable(BuiltInLootTables.FISHING);
        List<ItemStack> loot = table.getRandomItems(params);

        ItemFishedEvent event = new ItemFishedEvent(loot, hook.onGround() ? 2 : 1, hook);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            rod.hurtAndBreak(event.getRodDamage(), player, p -> p.broadcastBreakEvent(hand));
            hook.discard();
            return;
        }

        CriteriaTriggers.FISHING_ROD_HOOKED.trigger(player, rod, hook, loot);
        for (ItemStack stack : loot) {
            ItemEntity item = new ItemEntity(level, hook.getX(), hook.getY(), hook.getZ(), stack);
            double dx = player.getX() - hook.getX();
            double dy = player.getY() - hook.getY();
            double dz = player.getZ() - hook.getZ();
            item.setDeltaMovement(dx * 0.1, dy * 0.1 + Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * 0.08,
                    dz * 0.1);
            level.addFreshEntity(item);
            level.addFreshEntity(new ExperienceOrb(level, player.getX(), player.getY() + 0.5,
                    player.getZ() + 0.5, player.getRandom().nextInt(6) + 1));
            if (stack.is(ItemTags.FISHES)) {
                player.awardStat(Stats.FISH_CAUGHT, 1);
            }
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL, 1.0F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        rod.hurtAndBreak(event.getRodDamage(), player, p -> p.broadcastBreakEvent(hand));
        hook.discard();
    }

    public static void failure(ServerPlayer player, FishingHook hook) {
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL, 1.0F, 0.3F);
        hook.discard();
    }
}
