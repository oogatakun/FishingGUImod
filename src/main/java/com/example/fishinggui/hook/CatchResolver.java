package com.example.fishinggui.hook;

import com.example.fishinggui.fight.FishInfo;
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
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Resolves a finished fight: vanilla loot on success, nothing on failure. */
public final class CatchResolver {
    private CatchResolver() {
    }

    public static final int LINE_BREAK_ROD_DAMAGE = 5;

    public static List<ItemStack> roll(ServerPlayer player, FishingHook hook, ItemStack rod, float bonusLuck) {
        ServerLevel level = player.serverLevel();
        float luck = EnchantmentHelper.getFishingLuckBonus(rod) + player.getLuck() + bonusLuck;
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, hook.position())
                .withParameter(LootContextParams.TOOL, rod)
                .withParameter(LootContextParams.THIS_ENTITY, hook)
                .withParameter(LootContextParams.KILLER_ENTITY, player)
                .withLuck(luck)
                .create(LootContextParamSets.FISHING);
        LootTable table = level.getServer().getLootData().getLootTable(BuiltInLootTables.FISHING);
        return table.getRandomItems(params);
    }

    public static void success(ServerPlayer player, FishingHook hook, InteractionHand hand, List<ItemStack> loot,
            FishInfo info, float weight) {
        ServerLevel level = player.serverLevel();
        ItemStack rod = player.getItemInHand(hand);

        ItemFishedEvent event = new ItemFishedEvent(loot, hook.onGround() ? 2 : 1, hook);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            rod.hurtAndBreak(event.getRodDamage(), player, p -> p.broadcastBreakEvent(hand));
            hook.discard();
            return;
        }

        CriteriaTriggers.FISHING_ROD_HOOKED.trigger(player, rod, hook, loot);
        for (ItemStack stack : loot) {
            if (stack.is(ItemTags.FISHES)) {
                player.awardStat(Stats.FISH_CAUGHT, 1);
            }
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL, 1.0F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        rod.hurtAndBreak(event.getRodDamage(), player, p -> p.broadcastBreakEvent(hand));
        Vec3 origin = hook.position();
        hook.discard();
        if (!loot.isEmpty()) {
            PendingCatchManager.open(player, origin, loot, info, weight);
        }
    }

    public static void deliver(ServerPlayer player, Vec3 origin, List<ItemStack> loot, boolean vanillaXp) {
        ServerLevel level = player.serverLevel();
        for (ItemStack stack : loot) {
            ItemEntity item = new ItemEntity(level, origin.x, origin.y, origin.z, stack);
            double dx = player.getX() - origin.x;
            double dy = player.getY() - origin.y;
            double dz = player.getZ() - origin.z;
            item.setDeltaMovement(dx * 0.1, dy * 0.1 + Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * 0.08,
                    dz * 0.1);
            level.addFreshEntity(item);
            if (vanillaXp) {
                level.addFreshEntity(new ExperienceOrb(level, player.getX(), player.getY() + 0.5,
                        player.getZ() + 0.5, player.getRandom().nextInt(6) + 1));
            }
        }
    }

    public static void failure(ServerPlayer player, FishingHook hook, InteractionHand hand, boolean lineBroke) {
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL, 1.0F, 0.3F);
        if (lineBroke) {
            ItemStack rod = player.getItemInHand(hand);
            if (rod.getItem() instanceof FishingRodItem) {
                rod.hurtAndBreak(LINE_BREAK_ROD_DAMAGE, player, p -> p.broadcastBreakEvent(hand));
            }
        }
        hook.discard();
    }
}
