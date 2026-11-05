package com.example.fishinggui.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringUtil;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/** One serving of seafood hotpot in a bowl: grants FD's Nourishment for 5 minutes (when FD is loaded) and returns the bowl. */
public class HotpotBowlItem extends Item {
    private static final ResourceLocation NOURISHMENT = new ResourceLocation("farmersdelight", "nourishment");
    private static final int NOURISHMENT_TICKS = 5 * 60 * 20;

    public HotpotBowlItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        MobEffect nourishment = ForgeRegistries.MOB_EFFECTS.getValue(NOURISHMENT);
        if (nourishment != null) {
            tooltip.add(Component.translatable("potion.withDuration", nourishment.getDisplayName(),
                    StringUtil.formatTickDuration(NOURISHMENT_TICKS)).withStyle(nourishment.getCategory().getTooltipFormatting()));
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide) {
            MobEffect nourishment = ForgeRegistries.MOB_EFFECTS.getValue(NOURISHMENT);
            if (nourishment != null) {
                entity.addEffect(new MobEffectInstance(nourishment, NOURISHMENT_TICKS, 0, false, true));
            }
        }
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
