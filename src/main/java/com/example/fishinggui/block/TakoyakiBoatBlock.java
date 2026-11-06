package com.example.fishinggui.block;

import com.example.fishinggui.item.ModItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A boat of takoyaki set down on a surface; right-click eats one piece, an emptied boat stays until it is picked up (sneak) for the bowl. */
public class TakoyakiBoatBlock extends Block {
    public static final int MAX_PIECES = 6;
    public static final IntegerProperty PIECES = IntegerProperty.create("pieces", 0, MAX_PIECES);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 6, 15);

    public TakoyakiBoatBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PIECES, MAX_PIECES).setValue(FACING, Direction.SOUTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PIECES, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level,
            BlockPos pos, BlockPos neighborPos) {
        return dir == Direction.DOWN && !canSurvive(state, level, pos)
                ? Blocks.AIR.defaultBlockState() : super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        int pieces = state.getValue(PIECES);
        ItemStack held = player.getItemInHand(hand);
        boolean add = held.is(ModItems.TAKOYAKI.get()) && pieces < MAX_PIECES;
        boolean take = !add && player.isShiftKeyDown();
        boolean eat = !add && !take && pieces > 0;
        if (!add && !take && !eat) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (add) {
            // set one piece from the boat in hand down onto the placed one
            if (!player.getAbilities().instabuild) {
                if (held.getDamageValue() + 1 >= held.getMaxDamage()) {
                    player.setItemInHand(hand, new ItemStack(Items.BOWL));
                } else {
                    held.setDamageValue(held.getDamageValue() + 1);
                }
            }
            level.setBlock(pos, state.setValue(PIECES, pieces + 1), 3);
            level.playSound(null, pos, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.BLOCKS, 0.6f, 1.2f);
        } else if (take) {
            // sneak: pick the boat up (an empty one gives the bowl back)
            level.removeBlock(pos, false);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6f, 1.0f);
            ItemStack back = asItem(state);
            if (!player.getInventory().add(back)) {
                player.drop(back, false);
            }
        } else {
            player.getFoodData().eat(4, 0.7f);
            level.playSound(null, pos, SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.8f, 1.0f);
            level.setBlock(pos, state.setValue(PIECES, pieces - 1), 3);
        }
        return InteractionResult.CONSUME;
    }

    /** The boat as an item again, with the pieces that are left. */
    public static ItemStack asItem(BlockState state) {
        if (state.getValue(PIECES) == 0) {
            return new ItemStack(Items.BOWL);
        }
        ItemStack stack = new ItemStack(ModItems.TAKOYAKI.get());
        stack.setDamageValue(MAX_PIECES - state.getValue(PIECES));
        return stack;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(asItem(state));
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return asItem(state);
    }
}
