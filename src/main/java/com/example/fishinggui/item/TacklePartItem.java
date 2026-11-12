package com.example.fishinggui.item;

import com.example.fishinggui.tackle.Part;
import com.example.fishinggui.tackle.RodSetup;
import com.example.fishinggui.tackle.Style;
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
    private final Style style;

    public TacklePartItem(Part part, int rank, Style style, Properties properties) {
        super(properties);
        this.part = part;
        this.rank = rank;
        this.style = style;
    }

    public Part part() {
        return part;
    }

    public int rank() {
        return rank;
    }

    /** The fishing style of this part, or null for the plain part. */
    public Style style() {
        return style;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.fishinggui.rank." + rank).withStyle(RANK_COLORS[rank - 1]));
        if (style != null) {
            tooltip.add(Component.translatable("tooltip.fishinggui.style." + style.id()).withStyle(ChatFormatting.YELLOW));
        }
        tooltip.add(Component.translatable("tooltip.fishinggui.part." + part.id() + "." + rank)
                .withStyle(ChatFormatting.GRAY));
        if (style != null) {
            String key = part == Part.ROD_BLANK ? "rod" : part == Part.LURE ? "lure" : "power";
            double shown = style.power() * (part == Part.ROD_BLANK ? 300 : part == Part.LURE ? 200 : 100);
            tooltip.add(Component.translatable("tooltip.fishinggui.style." + key, signed(shown),
                    signed(part == Part.ROD_BLANK ? style.weightDelta() * 2 : style.weightDelta()))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (part != Part.ROD_BLANK) {
            tooltip.add(Component.translatable("tooltip.fishinggui.part.weight", RodSetup.weightOf(rank, style))
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
        tooltip.add(Component.translatable("tooltip.fishinggui.part.bench").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static String signed(double v) {
        return (v >= 0 ? "+" : "") + (v == Math.rint(v) ? String.valueOf((int) v) : String.format(java.util.Locale.ROOT, "%.0f", v));
    }
}
