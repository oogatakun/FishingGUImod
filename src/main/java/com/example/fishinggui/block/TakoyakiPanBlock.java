package com.example.fishinggui.block;

import com.example.fishinggui.food.TakoyakiRules;
import com.example.fishinggui.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A takoyaki pan. It heats while a lever next to it is on. Pour batter, add octopus, pour again, flip with an
 * ice pick, add toppings, then scoop the takoyaki up with a bowl.
 */
public class TakoyakiPanBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 6, 16);

    public TakoyakiPanBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(POWERED, leverOn(ctx.getLevel(), ctx.getClickedPos()));
    }

    /** True if a lever touching the pan is switched on. */
    private static boolean leverOn(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockState next = level.getBlockState(pos.relative(dir));
            if (next.getBlock() instanceof LeverBlock && next.getValue(LeverBlock.POWERED)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from,
            boolean moving) {
        if (level.isClientSide) {
            return;
        }
        boolean on = leverOn(level, pos);
        if (on != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, on), 3);
            level.playSound(null, pos, on ? SoundEvents.FIRE_AMBIENT : SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.BLOCKS, 0.5f, on ? 1.4f : 1.0f);
        }
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
        return new TakoyakiPanBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlocks.TAKOYAKI_PAN_BE.get(),
                level.isClientSide ? TakoyakiPanBlockEntity::clientTick : TakoyakiPanBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        boolean tool = held.is(ModItems.TAKOYAKI_BATTER.get()) || held.is(ModItems.CUT_OCTOPUS.get())
                || held.is(ModItems.ICE_PICK.get()) || held.is(ModItems.MAYONNAISE.get())
                || held.is(ModItems.AONORI.get()) || held.is(Items.BOWL);
        if (!tool || !(level.getBlockEntity(pos) instanceof TakoyakiPanBlockEntity be)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        boolean creative = player.getAbilities().instabuild;
        int n;
        SoundEvent sound = SoundEvents.BOTTLE_EMPTY;
        if (held.is(ModItems.TAKOYAKI_BATTER.get())) {
            n = be.pour();
            if (n > 0 && !creative) {
                held.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
            }
        } else if (held.is(ModItems.CUT_OCTOPUS.get())) {
            n = be.addOctopus(creative ? TakoyakiRules.WELLS : held.getCount());
            if (n > 0 && !creative) {
                held.shrink(n);
            }
            sound = SoundEvents.SLIME_BLOCK_PLACE;
        } else if (held.is(ModItems.ICE_PICK.get())) {
            n = be.flip();
            if (n > 0 && !creative) {
                held.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
            }
            sound = SoundEvents.ITEM_FRAME_ROTATE_ITEM;
        } else if (held.is(ModItems.MAYONNAISE.get()) || held.is(ModItems.AONORI.get())) {
            n = be.topping(held.is(ModItems.MAYONNAISE.get()) ? TakoyakiRules.MAYO : TakoyakiRules.AONORI);
            if (n > 0 && !creative) {
                if (!held.isDamageableItem()) {
                    held.shrink(1);
                } else if (held.getDamageValue() + 1 >= held.getMaxDamage()) {
                    player.setItemInHand(hand, new ItemStack(Items.GLASS_BOTTLE));
                } else {
                    held.setDamageValue(held.getDamageValue() + 1);
                }
            }
            sound = SoundEvents.HONEY_BLOCK_SLIDE;
        } else {
            n = scoop(be, player, held, creative);
            sound = SoundEvents.ITEM_PICKUP;
        }
        if (n == 0) {
            String key = held.is(Items.BOWL) && be.readyWithoutTopping()
                    ? "gui.fishinggui.takoyaki.no_topping" : "gui.fishinggui.takoyaki.nothing";
            player.displayClientMessage(Component.translatable(key), true);
            return InteractionResult.CONSUME;
        }
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 0.8f, 1.0f);
        return InteractionResult.CONSUME;
    }

    /** Bowl in hand: scoop up to one boat of finished takoyaki. */
    private static int scoop(TakoyakiPanBlockEntity be, Player player, ItemStack bowl, boolean creative) {
        int n = be.collect(TakoyakiRules.PORTIONS);
        if (n == 0) {
            return 0;
        }
        if (!creative) {
            bowl.shrink(1);
        }
        ItemStack boat = new ItemStack(ModItems.TAKOYAKI.get());
        boat.setDamageValue(TakoyakiRules.PORTIONS - n);
        if (!player.getInventory().add(boat)) {
            player.drop(boat, false);
        }
        return n;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(POWERED) || !(level.getBlockEntity(pos) instanceof TakoyakiPanBlockEntity be)
                || !be.anyCooking()) {
            return;
        }
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.2 + random.nextDouble() * 0.6,
                    pos.getY() + 0.5, pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.03, 0);
        }
        if (random.nextInt(20) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5,
                    SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.4f, 1.3f, false);
        }
    }

    /** Block item with a faint hint about the lever. */
    public static BlockItem item(Block block, net.minecraft.world.item.Item.Properties props) {
        return new BlockItem(block, props) {
            @Override
            public void appendHoverText(ItemStack stack, Level level, java.util.List<Component> tooltip,
                    net.minecraft.world.item.TooltipFlag flag) {
                tooltip.add(Component.translatable("tooltip.fishinggui.takoyaki_pan"));
            }
        };
    }
}
