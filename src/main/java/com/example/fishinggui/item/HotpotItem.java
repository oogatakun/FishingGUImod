package com.example.fishinggui.item;

import com.example.fishinggui.block.SeafoodHotpotBlock;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** The seafood hotpot pot: only placed, never eaten directly (serve it into bowls). */
public class HotpotItem extends BlockItem {
    private final boolean empty;

    public HotpotItem(Block block, Properties properties, boolean empty) {
        super(block, properties);
        this.empty = empty;
    }

    @Override
    @Nullable
    protected BlockState getPlacementState(BlockPlaceContext ctx) {
        BlockState state = super.getPlacementState(ctx);
        return state != null && empty ? state.setValue(SeafoodHotpotBlock.SERVINGS, 0) : state;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.fishinggui.seafood_hotpot").withStyle(ChatFormatting.DARK_GRAY));
    }
}
