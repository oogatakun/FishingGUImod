package com.example.fishinggui.client;

import com.example.fishinggui.FishingGuiMod;
import com.example.fishinggui.item.CookedFishItem;
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
        if (key != null && FishingGuiMod.MOD_ID.equals(key.getNamespace()) && key.getPath().startsWith("cooked_")) {
            String raw = key.getPath().substring("cooked_".length());
            int heal = CookedFishItem.healFor(raw);
            if (heal > 0) {
                String hearts = heal % 2 == 0 ? String.valueOf(heal / 2) : String.valueOf(heal / 2.0);
                event.getToolTip().add(Component.translatable("tooltip.fishinggui.cooked_heal", hearts)
                        .withStyle(ChatFormatting.GRAY));
            }
            Integer rawLevel = FishLevels.BY_ID.get(FishingGuiMod.MOD_ID + ":" + raw);
            if (rawLevel != null) {
                event.getToolTip().add(Component.translatable("tooltip.fishinggui.fish_level", rawLevel)
                        .withStyle(ChatFormatting.AQUA));
            }
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
