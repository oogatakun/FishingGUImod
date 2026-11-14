package com.example.fishinggui.client;

import com.example.fishinggui.FishingGuiMod;
import com.example.fishinggui.sieve.SieveItem;
import com.example.fishinggui.sieve.SieveItems;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = FishingGuiMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SieveClientSetup {
    private SieveClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(SieveItems.SIEVE.get(),
                new ResourceLocation(FishingGuiMod.MOD_ID, "load"), (stack, level, entity, seed) -> load(stack, entity)));
    }

    /**
     * What the sieve holds while sifting, for the model overrides in sieve.json: material * 10 + fill level,
     * where material is 1 dirt, 2 gravel, 3 sand, 4 red sand and the fill goes 3 (just put in) to 1 (nearly
     * through). 0 when empty.
     */
    private static float load(ItemStack stack, LivingEntity entity) {
        if (entity == null || !entity.isUsingItem() || entity.getUsedItemHand() != InteractionHand.MAIN_HAND
                || entity.getUseItem() != stack) {
            return 0;
        }
        ItemStack material = entity.getOffhandItem();
        int id;
        if (material.is(Items.DIRT)) {
            id = 1;
        } else if (material.is(Items.GRAVEL)) {
            id = 2;
        } else if (material.is(Items.SAND)) {
            id = 3;
        } else if (material.is(Items.RED_SAND)) {
            id = 4;
        } else {
            return 0;
        }
        float progress = Mth.clamp(entity.getTicksUsingItem() / (float) SieveItem.SIFT_TICKS, 0.0F, 1.0F);
        int fill = progress < 1.0F / 3 ? 3 : progress < 2.0F / 3 ? 2 : 1;
        return id * 10 + fill;
    }
}
