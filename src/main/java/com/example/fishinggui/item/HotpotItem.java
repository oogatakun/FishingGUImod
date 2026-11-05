package com.example.fishinggui.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/** Seafood hotpot: eaten as a bowl food, or placed as a block with sneak + right-click. */
public class HotpotItem extends BlockItem {
    public HotpotItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        return player != null && player.isSecondaryUseActive() ? super.useOn(ctx) : InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.fishinggui.seafood_hotpot"));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (entity instanceof Player player && player.getAbilities().instabuild) {
            return result;
        }
        if (result.isEmpty()) {
            return new ItemStack(Items.BOWL);
        }
        if (entity instanceof Player player && !player.getInventory().add(new ItemStack(Items.BOWL))) {
            player.drop(new ItemStack(Items.BOWL), false);
        }
        return result;
    }
}
