package com.example.fishinggui.entity;

import com.example.fishinggui.item.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

/**
 * An earthworm on the ground, an animal like a chicken: fed rotten flesh it falls in love, two in love on dirt breed
 * (two young at a time, which grow up in time), and the parents wait five minutes before the next. It is picked up
 * again by right-clicking it with a fishing rod or a bug net. It never spawns by itself and never despawns.
 */
public class WormEntity extends Animal {
    /** Entity event: the worm coils up for a moment. */
    private static final byte CURL_EVENT = 100;
    /** Chance per tick that a still worm curls up (about once in half a minute). */
    private static final int CURL_ONE_IN = 600;

    private int curlStart = -1000;

    public WormEntity(EntityType<? extends WormEntity> type, Level level) {
        super(type, level);
        setMaxUpStep(WormBreeding.JUMP_HEIGHT);
    }

    /** Takes off fast enough to rise about half a block (the vanilla 0.42 reaches a block and a quarter). */
    @Override
    protected float getJumpPower() {
        return 0.27F * getBlockJumpFactor();
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 2.0).add(Attributes.MOVEMENT_SPEED, 0.05);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new BreedGoal(this, 1.0));
        goalSelector.addGoal(2, new TemptGoal(this, 1.1, Ingredient.of(Items.ROTTEN_FLESH), false));
        goalSelector.addGoal(3, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0, 0.002F));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean removeWhenFarAway(double distanceSqr) {
        return false;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.ROTTEN_FLESH);
    }

    private boolean onDirt() {
        return level().getBlockState(blockPosition().below()).is(BlockTags.DIRT);
    }

    /** Two worms in love breed only when both are on dirt. */
    @Override
    public boolean canMate(Animal other) {
        return super.canMate(other) && onDirt() && other instanceof WormEntity worm && worm.onDirt();
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.WORM.get().create(level);
    }

    /** As for any animal, plus the rest of the brood. */
    @Override
    public void spawnChildFromBreeding(ServerLevel level, Animal mate) {
        super.spawnChildFromBreeding(level, mate);
        for (int i = 1; i < WormBreeding.OFFSPRING; i++) {
            AgeableMob young = getBreedOffspring(level, mate);
            if (young != null) {
                young.setBaby(true);
                young.moveTo(getX(), getY(), getZ(), 0.0F, 0.0F);
                level.addFreshEntityWithPassengers(young);
            }
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        boolean net = held.is(ModItems.BUG_NET.get());
        if (!(held.getItem() instanceof FishingRodItem) && !net) {
            return super.mobInteract(player, hand);
        }
        if (!level().isClientSide) {
            ItemStack worm = new ItemStack(ModItems.WORM.get());
            if (!player.getInventory().add(worm)) {
                player.drop(worm, false);
            }
            level().playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH_SMALL, SoundSource.NEUTRAL, 0.6F, 1.4F);
            if (net) {
                held.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
            }
            discard();
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    /** How far into its curl the worm is, in seconds at {@code ageInTicks}; negative or past the end when not curling. */
    public float curlSeconds(float ageInTicks) {
        return (ageInTicks - curlStart) / 20.0F;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == CURL_EVENT) {
            curlStart = tickCount;
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (random.nextInt(CURL_ONE_IN) == 0 && getNavigation().isDone()) {
            level().broadcastEntityEvent(this, CURL_EVENT);
        }
    }
}
