package com.example.fishinggui.block;

import com.example.fishinggui.food.CookingRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.ForgeRegistries;

public class ShichirinBlock extends BaseEntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** Set while lit but nearly out of fuel: dimmer glow and fewer sparks. */
    public static final BooleanProperty WEAK = BooleanProperty.create("weak");
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 10, 14);

    public ShichirinBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false).setValue(WEAK, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, WEAK);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShichirinBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, ModBlocks.SHICHIRIN_BE.get(), ShichirinBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ShichirinBlockEntity be)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        boolean lit = state.getValue(LIT);
        if (held.is(Items.CHARCOAL) || held.is(Items.COAL)) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            boolean coal = held.is(Items.COAL);
            if (be.tryAddFuel(coal)) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                level.playSound(null, pos, coal ? SoundEvents.GRAVEL_PLACE : SoundEvents.WOOD_PLACE,
                        SoundSource.BLOCKS, 0.8f, coal ? 0.9f : 1.2f);
                if (level instanceof ServerLevel sl) {
                    sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM,
                            new ItemStack(coal ? Items.COAL : Items.CHARCOAL)),
                            pos.getX() + 0.5, pos.getY() + 0.55, pos.getZ() + 0.5, 8, 0.15, 0.05, 0.15, 0.05);
                }
            }
            return InteractionResult.CONSUME;
        }
        if (held.is(Items.FLINT_AND_STEEL)) {
            if (lit) {
                return InteractionResult.PASS;
            }
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            if (be.fuelTicks() <= 0) {
                player.displayClientMessage(Component.translatable("gui.fishinggui.shichirin.no_fuel"), true);
                return InteractionResult.CONSUME;
            }
            level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0f,
                    level.getRandom().nextFloat() * 0.4f + 0.8f);
            level.setBlock(pos, state.setValue(LIT, true)
                    .setValue(WEAK, be.fuelTicks() < ShichirinBlockEntity.WEAK_TICKS), 3);
            held.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 10,
                        0.15, 0.05, 0.15, 0.02);
            }
            return InteractionResult.CONSUME;
        }
        if (held.is(Items.WATER_BUCKET)) {
            if (!lit) {
                return InteractionResult.PASS;
            }
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            level.setBlock(pos, state.setValue(LIT, false).setValue(WEAK, false), 3);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 1.0f);
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.6f, 1.0f);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5,
                        14, 0.2, 0.1, 0.2, 0.03);
                sl.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5,
                        8, 0.2, 0.1, 0.2, 0.05);
            }
            return InteractionResult.CONSUME;
        }
        if (!held.isEmpty()) {
            var key = ForgeRegistries.ITEMS.getKey(held.getItem());
            if (key == null || !CookingRules.isGrillable(key.toString())) {
                return InteractionResult.PASS;
            }
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            if (be.tryPlace(held) && !player.getAbilities().instabuild) {
                held.shrink(1);
            }
            return InteractionResult.CONSUME;
        }
        if (!player.isShiftKeyDown() || hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ItemStack out = be.takeOutNear(hit.getLocation().x - pos.getX(), hit.getLocation().z - pos.getZ());
        if (!out.isEmpty() && !player.getInventory().add(out)) {
            player.drop(out, false);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())
                && !level.isClientSide && level.getBlockEntity(pos) instanceof ShichirinBlockEntity be) {
            for (ItemStack s : be.contents()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s);
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        boolean weak = state.getValue(WEAK);
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.55;
        double z = pos.getZ() + 0.5;
        if (random.nextInt(weak ? 5 : 8) == 0) {
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.35 + random.nextDouble() * 0.3,
                    pos.getY() + 0.75, pos.getZ() + 0.35 + random.nextDouble() * 0.3, 0, 0.03, 0);
        }
        // Embers drifting up out of the coals; sparse and short-lived when the fire is dying.
        if (random.nextInt(weak ? 12 : 3) == 0) {
            level.addParticle(weak ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME,
                    x + (random.nextDouble() - 0.5) * 0.4, y, z + (random.nextDouble() - 0.5) * 0.4,
                    (random.nextDouble() - 0.5) * 0.02, 0.03 + random.nextDouble() * 0.03,
                    (random.nextDouble() - 0.5) * 0.02);
        }
        if (!weak && random.nextInt(10) == 0) {
            level.addParticle(ParticleTypes.LAVA, x + (random.nextDouble() - 0.5) * 0.3, y,
                    z + (random.nextDouble() - 0.5) * 0.3, 0, 0, 0);
        }
        if (!weak && random.nextInt(24) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.4f,
                    0.8f + random.nextFloat() * 0.4f, false);
        }
    }
}
