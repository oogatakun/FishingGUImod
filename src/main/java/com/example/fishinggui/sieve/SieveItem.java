package com.example.fishinggui.sieve;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.example.fishinggui.FishingGuiMod;
import com.example.fishinggui.item.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * A hand sieve. With dirt, sand or gravel in the off hand, hold use with the sieve in the main hand: the sieve
 * shakes for a moment, one of the off-hand blocks is used up, and the ore bits it held (now and then a worm)
 * go into the player's inventory.
 */
public class SieveItem extends Item {
    /** How long one sift takes, in ticks. */
    public static final int SIFT_TICKS = 30;
    /**
     * The client counts its own sift and starts the next one as soon as it is done, which can reach the server a
     * tick or two before the server's own count is done. A new start that late in a sift finishes it first.
     */
    private static final int FINISH_TOLERANCE_TICKS = 3;
    /** Items that can be sifted from the off hand (data/fishinggui/tags/items/siftable.json). */
    public static final TagKey<Item> SIFTABLE = ItemTags.create(new ResourceLocation(FishingGuiMod.MOD_ID, "siftable"));

    public SieveItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack sieve = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(sieve);
        }
        if (isWornOut(sieve)) {
            if (level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.fishinggui.worn_out"), true);
            }
            return InteractionResultHolder.fail(sieve);
        }
        if (!player.getOffhandItem().is(SIFTABLE)) {
            if (level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.fishinggui.need_material"), true);
            }
            return InteractionResultHolder.fail(sieve);
        }
        if (!level.isClientSide && player.isUsingItem() && player.getUseItem().is(this)
                && elapsed(sieve, player.getUseItemRemainingTicks()) >= SIFT_TICKS - FINISH_TOLERANCE_TICKS) {
            finishSift(level, player, sieve);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(sieve);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.CUSTOM;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    private int elapsed(ItemStack sieve, int remaining) {
        return getUseDuration(sieve) - remaining;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(entity instanceof Player player) || entity.getUsedItemHand() != InteractionHand.MAIN_HAND) {
            entity.releaseUsingItem();
            return;
        }
        ItemStack material = player.getOffhandItem();
        if (!material.is(SIFTABLE) || isWornOut(stack)) {
            entity.releaseUsingItem();
            return;
        }
        BlockState state = Block.byItem(material.getItem()).defaultBlockState();
        int elapsed = elapsed(stack, remaining) + 1;
        if (level.isClientSide) {
            spawnSiftParticles(level, player, state);
        }
        if (elapsed % 6 == 1) {
            level.playSound(player, player.getX(), player.getY(), player.getZ(), siftSound(material),
                    SoundSource.PLAYERS, 0.8F, 0.9F + level.random.nextFloat() * 0.2F);
        }
        if (elapsed >= SIFT_TICKS) {
            if (!level.isClientSide) {
                finishSift(level, player, stack);
            }
            entity.releaseUsingItem();
        }
    }

    private void finishSift(Level level, Player player, ItemStack sieve) {
        ItemStack material = player.getOffhandItem();
        if (!material.is(SIFTABLE)) {
            return;
        }
        if (!player.getAbilities().instabuild) {
            material.shrink(1);
        }
        SieveDrops.Result drops = SieveDrops.roll(level.random);
        for (SieveOre ore : SieveOre.values()) {
            int count = drops.bits()[ore.ordinal()];
            if (count > 0) {
                ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(SieveItems.bit(ore), count));
            }
        }
        if (drops.worm()) {
            ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(ModItems.WORM.get()));
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        wear(player, sieve);
    }

    /** A sieve with no durability left stays in the inventory but can't sift until it is repaired. */
    public static boolean isWornOut(ItemStack sieve) {
        return sieve.isDamageableItem() && sieve.getDamageValue() >= sieve.getMaxDamage();
    }

    /**
     * Like {@link ItemStack#hurtAndBreak}, Unbreaking included, but the sieve is never used up: at zero durability
     * it plays the breaking sound and particles and stays, worn out.
     */
    private static void wear(Player player, ItemStack sieve) {
        if (player.getAbilities().instabuild || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (sieve.hurt(1, player.getRandom(), serverPlayer)) {
            sieve.setDamageValue(sieve.getMaxDamage());
            player.broadcastBreakEvent(InteractionHand.MAIN_HAND);
            player.displayClientMessage(Component.translatable("message.fishinggui.worn_out"), true);
        }
    }

    private static SoundEvent siftSound(ItemStack material) {
        if (material.is(ItemTags.SAND)) {
            return SoundEvents.BRUSH_SAND;
        }
        if (material.is(Items.GRAVEL)) {
            return SoundEvents.BRUSH_GRAVEL;
        }
        return SoundEvents.BRUSH_GENERIC;
    }

    /** Small grains of the material drop out of the bottom of the sieve, in front of the player. */
    private static void spawnSiftParticles(Level level, Player player, BlockState state) {
        RandomSource random = level.random;
        if (random.nextInt(2) != 0) {
            return;
        }
        // Just under where the sieve sits on screen, a little below the middle of the view; the sieve is drawn over
        // the world, so the grains show as falling out of its bottom.
        Vec3 toSieve = Vec3.directionFromRotation(Math.min(player.getXRot() + 24.0F, 85.0F), player.getYRot());
        Vec3 under = player.getEyePosition().add(toSieve.scale(1.4));
        com.example.fishinggui.client.SieveClientExtensions.spawnGrain(state,
                under.x + (random.nextDouble() - 0.5) * 0.4,
                under.y,
                under.z + (random.nextDouble() - 0.5) * 0.4);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(Items.STRING);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.fishinggui.sieve.tooltip").withStyle(ChatFormatting.GRAY));
        if (isWornOut(stack)) {
            tooltip.add(Component.translatable("item.fishinggui.sieve.worn_out").withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(com.example.fishinggui.client.SieveClientExtensions.INSTANCE);
    }
}
