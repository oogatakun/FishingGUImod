package com.example.fishinggui.item;

import com.example.fishinggui.tackle.Part;
import com.example.fishinggui.tackle.RodSetup;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** A rod assembled at a tackle bench: the fitted parts live in the stack's NBT and decide its numbers. */
public class TackleRodItem extends FishingRodItem {
    private static final String TAG = "Tackle";

    public TackleRodItem(Properties properties) {
        super(properties);
    }

    /** The parts fitted to a rod stack; empty if it is not a tackle rod. */
    public static RodSetup setupOf(ItemStack stack) {
        RodSetup setup = new RodSetup();
        if (!(stack.getItem() instanceof TackleRodItem) || !stack.hasTag()) {
            return setup;
        }
        CompoundTag tag = stack.getTagElement(TAG);
        if (tag == null) {
            return setup;
        }
        for (Part part : Part.values()) {
            setup = setup.with(part, tag.getInt(part.id()));
        }
        return tag.contains("LureSpecies") ? setup.withLureSpecies(tag.getString("LureSpecies")) : setup;
    }

    /** Builds the rod item for a setup; {@code damage} carries the wear of the fitted rod blank. */
    public static ItemStack assemble(RodSetup setup, int damage) {
        ItemStack stack = new ItemStack(ModItems.TACKLE_ROD.get());
        CompoundTag tag = new CompoundTag();
        for (Part part : Part.values()) {
            if (setup.rank(part) > 0) {
                tag.putInt(part.id(), setup.rank(part));
            }
        }
        if (setup.lureSpecies() != null) {
            tag.putString("LureSpecies", setup.lureSpecies());
        }
        stack.getOrCreateTag().put(TAG, tag);
        stack.getOrCreateTag().putInt("CustomModelData", Math.max(1, setup.rank(Part.ROD_BLANK)));
        stack.setDamageValue(Math.max(0, Math.min(setup.durability() - 1, damage)));
        return stack;
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        int max = setupOf(stack).durability();
        return max > 0 ? max : super.getMaxDamage(stack);
    }

    /** The number a fitted part contributes, as shown next to its name; null if it has none. */
    private static String statOf(RodSetup setup, Part part) {
        return switch (part) {
            case ROD_BLANK -> "Lv." + setup.levelCap() + " / " + setup.durability();
            case REEL -> String.format(java.util.Locale.ROOT, "x%.2f", setup.reelSpeed());
            case LINE -> String.format(java.util.Locale.ROOT, "x%.2f", setup.tensionScale());
            case HOOK -> "+" + (int) setup.hookLuck();
            case FLOAT -> "-" + Math.round(setup.biteCut() * 100) + "%";
            case LEADER -> String.format(java.util.Locale.ROOT, "x%.2f", setup.dashScale());
            case LURE -> Math.round(RodSetup.LURE_CHANCE * 100) + "%";
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        RodSetup setup = setupOf(stack);
        for (Part part : Part.values()) {
            if (!part.available()) {
                continue;
            }
            int rank = setup.rank(part);
            Component name = rank > 0
                    ? Component.translatable("item.fishinggui." + part.itemName(rank))
                    : Component.translatable("tooltip.fishinggui.rod.none").withStyle(ChatFormatting.DARK_GRAY);
            net.minecraft.network.chat.MutableComponent line = Component.translatable(
                    "tooltip.fishinggui.rod.slot." + part.id(), name).withStyle(ChatFormatting.GRAY);
            String stat = rank > 0 ? statOf(setup, part) : null;
            if (stat != null) {
                line.append(Component.literal("  " + stat).withStyle(ChatFormatting.GREEN));
            }
            tooltip.add(line);
        }
        if (setup.lureSpecies() != null) {
            net.minecraft.world.item.Item fish = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                    new net.minecraft.resources.ResourceLocation(setup.lureSpecies()));
            if (fish != null) {
                tooltip.add(Component.translatable("tooltip.fishinggui.lure.species", fish.getDescription())
                        .withStyle(ChatFormatting.AQUA));
            }
        }
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.fishinggui.rod.level_cap", setup.levelCap())
                .withStyle(ChatFormatting.GREEN));
    }
}
