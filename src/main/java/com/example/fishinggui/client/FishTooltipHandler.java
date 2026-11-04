package com.example.fishinggui.client;

import com.example.fishinggui.FishingGuiMod;
import com.example.fishinggui.item.FishLevels;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Adds the fish level and the bait requirement to the tooltip of every fish / bait item. */
@Mod.EventBusSubscriber(modid = FishingGuiMod.MOD_ID, value = Dist.CLIENT)
public final class FishTooltipHandler {
    private FishTooltipHandler() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        var key = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        if (key != null && "fishinggui:reset_drop".equals(key.toString())) {
            event.getToolTip().add(Component.translatable("item.fishinggui.reset_drop.desc")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        Integer level = key == null ? null : FishLevels.BY_ID.get(key.toString());
        if (level == null) {
            return;
        }
        if (!"fishinggui:worm".equals(key.toString())) {
            event.getToolTip().add(Component.translatable("tooltip.fishinggui.fish_level", level)
                    .withStyle(ChatFormatting.AQUA));
        }
        event.getToolTip().add(Component.translatable("tooltip.fishinggui.bait_level", level)
                .withStyle(ChatFormatting.GRAY));
    }
}
