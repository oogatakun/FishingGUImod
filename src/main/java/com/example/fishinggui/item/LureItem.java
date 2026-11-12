package com.example.fishinggui.item;

import com.example.fishinggui.data.FishProfileLoader;
import com.example.fishinggui.fight.FishInfo;
import com.example.fishinggui.tackle.Part;
import com.example.fishinggui.tackle.RodSetup;
import com.example.fishinggui.tackle.Style;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/** A lure. Use it with a fish in the other hand to set the species it calls; fitted to a rod it calls it 10% of the time. */
public class LureItem extends TacklePartItem {
    private static final String SPECIES = "Species";

    public LureItem(int rank, Style style, Properties properties) {
        super(Part.LURE, rank, style, properties);
    }

    @Nullable
    public static String speciesOf(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(SPECIES) ? stack.getTag().getString(SPECIES) : null;
    }

    public static void setSpecies(ItemStack stack, String species) {
        stack.getOrCreateTag().putString(SPECIES, species);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack lure = player.getItemInHand(hand);
        ItemStack fish = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND
                : InteractionHand.MAIN_HAND);
        if (level.isClientSide || fish.isEmpty() || FishProfileLoader.baitLevelOf(fish) <= 0) {
            return InteractionResultHolder.pass(lure);
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(fish.getItem());
        FishInfo info = FishProfileLoader.infoFor(id);
        if (info.catchLevel() > rank() * RodSetup.LURE_LEVELS_PER_RANK) {
            player.displayClientMessage(Component.translatable("gui.fishinggui.lure.too_high",
                    rank() * RodSetup.LURE_LEVELS_PER_RANK), true);
            return InteractionResultHolder.fail(lure);
        }
        ItemStack one = lure.split(1);
        setSpecies(one, id.toString());
        if (!player.getAbilities().instabuild) {
            fish.shrink(1);
        }
        if (!player.getInventory().add(one)) {
            player.drop(one, false);
        }
        player.displayClientMessage(Component.translatable("gui.fishinggui.lure.set", fish.getHoverName()), true);
        return InteractionResultHolder.success(lure);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        String species = speciesOf(stack);
        if (species == null) {
            tooltip.add(Component.translatable("tooltip.fishinggui.lure.unset").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        net.minecraft.world.item.Item fish = ForgeRegistries.ITEMS.getValue(new ResourceLocation(species));
        if (fish != null) {
            tooltip.add(Component.translatable("tooltip.fishinggui.lure.species", fish.getDescription())
                    .withStyle(ChatFormatting.AQUA));
        }
    }
}
