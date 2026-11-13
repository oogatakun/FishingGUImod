package com.example.fishinggui.entity;

import com.example.fishinggui.item.ModItems;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * An earthworm on the ground. It crawls about, is picked up again by right-clicking it with a fishing rod or a bug net, and two of
 * them close together on dirt breed now and then. It never spawns by itself and never despawns.
 */
public class WormEntity extends PathfinderMob {
    private static final String COOLDOWN_TAG = "BreedCooldown";

    private int breedCooldown = WormBreeding.SETTLE;

    public WormEntity(EntityType<? extends WormEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 2.0).add(Attributes.MOVEMENT_SPEED, 0.05);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 1.0, 0.002F));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean removeWhenFarAway(double distanceSqr) {
        return false;
    }

    public void setBreedCooldown(int ticks) {
        breedCooldown = Math.max(0, ticks);
    }

    private boolean onDirt() {
        return level().getBlockState(blockPosition().below()).is(BlockTags.DIRT);
    }

    private boolean readyToBreed() {
        return isAlive() && breedCooldown <= 0 && onDirt();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
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

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (breedCooldown > 0) {
            breedCooldown--;
        } else if (tickCount % 40 == 0) {
            tryBreed();
        }
    }

    private void tryBreed() {
        if (!(level() instanceof ServerLevel server) || !readyToBreed()) {
            return;
        }
        List<WormEntity> partners = server.getEntitiesOfClass(WormEntity.class,
                getBoundingBox().inflate(WormBreeding.PARTNER_RANGE, 2.0, WormBreeding.PARTNER_RANGE),
                w -> w != this && w.readyToBreed());
        if (partners.isEmpty()) {
            return;
        }
        WormEntity partner = partners.get(0);
        breedCooldown = WormBreeding.COOLDOWN;
        partner.breedCooldown = WormBreeding.COOLDOWN;
        double x = (getX() + partner.getX()) / 2.0;
        double y = Math.max(getY(), partner.getY());
        double z = (getZ() + partner.getZ()) / 2.0;
        for (int i = 0; i < WormBreeding.OFFSPRING; i++) {
            WormEntity young = ModEntities.WORM.get().create(server);
            if (young != null) {
                young.moveTo(x + (random.nextDouble() - 0.5) * 0.4, y, z + (random.nextDouble() - 0.5) * 0.4,
                        random.nextFloat() * 360.0F, 0.0F);
                young.setBreedCooldown(WormBreeding.SETTLE);
                server.addFreshEntity(young);
            }
        }
        server.sendParticles(ParticleTypes.HEART, x, y + 0.2, z, 5, 0.2, 0.1, 0.2, 0.0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(COOLDOWN_TAG, breedCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(COOLDOWN_TAG)) {
            breedCooldown = tag.getInt(COOLDOWN_TAG);
        }
    }
}
