package com.example.fishinggui.item;

import com.example.fishinggui.block.TakoyakiBoatBlock;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A boat of takoyaki: each bite eats one piece and the damage bar shows how many are left. Sneak and use it on a
 * block to set it down; the placed boat keeps the pieces that are left.
 */
public class TakoyakiItem extends BlockItem {
    public TakoyakiItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        return player != null && player.isShiftKeyDown() ? place(new BlockPlaceContext(ctx)) : InteractionResult.PASS;
    }

    @Override
    @Nullable
    protected BlockState getPlacementState(BlockPlaceContext ctx) {
        BlockState state = super.getPlacementState(ctx);
        ItemStack stack = ctx.getItemInHand();
        return state == null ? null
                : state.setValue(TakoyakiBoatBlock.PIECES, TakoyakiBoatBlock.MAX_PIECES - stack.getDamageValue());
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return super.finishUsingItem(stack, level, entity);
        }
        player.eat(level, stack.copy());
        if (player.getAbilities().instabuild) {
            return stack;
        }
        if (stack.getDamageValue() + 1 >= stack.getMaxDamage()) {
            return new ItemStack(Items.BOWL);
        }
        stack.setDamageValue(stack.getDamageValue() + 1);
        return stack;
    }
}
