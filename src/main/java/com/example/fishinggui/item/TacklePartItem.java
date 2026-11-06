package com.example.fishinggui.item;

import com.example.fishinggui.tackle.Part;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** One part of a rod at one rank; it is fitted at a tackle bench. */
public class TacklePartItem extends Item {
    private static final ChatFormatting[] RANK_COLORS = {ChatFormatting.GOLD, ChatFormatting.WHITE,
            ChatFormatting.AQUA, ChatFormatting.DARK_PURPLE};

    private final Part part;
    private final int rank;

    public TacklePartItem(Part part, int rank, Properties properties) {
        super(properties);
        this.part = part;
        this.rank = rank;
    }

    public Part part() {
        return part;
    }

    public int rank() {
        return rank;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.fishinggui.rank." + rank).withStyle(RANK_COLORS[rank - 1]));
        tooltip.add(Component.translatable("tooltip.fishinggui.part." + part.id() + "." + rank)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.fishinggui.part.bench").withStyle(ChatFormatting.DARK_GRAY));
    }
}
