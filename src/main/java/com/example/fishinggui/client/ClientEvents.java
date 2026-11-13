package com.example.fishinggui.client;

import com.example.fishinggui.FishingGuiMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FishingGuiMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(net.minecraft.world.entity.EntityType.FISHING_BOBBER, TackleHookRenderer::new);
        event.registerEntityRenderer(com.example.fishinggui.entity.ModEntities.WORM.get(), WormRenderer::new);
        event.registerBlockEntityRenderer(com.example.fishinggui.block.ModBlocks.SHICHIRIN_BE.get(),
                ShichirinRenderer::new);
        event.registerBlockEntityRenderer(com.example.fishinggui.block.ModBlocks.TAKOYAKI_PAN_BE.get(),
                TakoyakiPanRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterLayers(
            net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WormModel.LAYER, WormModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onClientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            net.minecraft.client.gui.screens.MenuScreens.register(
                    com.example.fishinggui.block.ModMenus.TACKLE_BENCH.get(), TackleBenchScreen::new);
            // the casting sprite, the same rule as the vanilla rod
            net.minecraft.client.renderer.item.ItemProperties.register(
                    com.example.fishinggui.item.ModItems.TACKLE_ROD.get(),
                    new net.minecraft.resources.ResourceLocation("cast"),
                    (stack, level, entity, seed) -> isCast(stack, entity) ? 1.0f : 0.0f);
        });
    }

    /** True while the player holds this rod with the line out; the same rule as the vanilla rod's cast sprite. */
    public static boolean isCast(net.minecraft.world.item.ItemStack stack,
            @org.jetbrains.annotations.Nullable net.minecraft.world.entity.LivingEntity entity) {
        if (!(entity instanceof net.minecraft.world.entity.player.Player player)) {
            return false;
        }
        boolean main = player.getMainHandItem() == stack;
        boolean off = player.getOffhandItem() == stack
                && !(player.getMainHandItem().getItem() instanceof net.minecraft.world.item.FishingRodItem);
        return (main || off) && player.fishing != null;
    }

    /** Swaps the tackle rod's model for one that is put together from its parts. */
    @SubscribeEvent
    public static void onModifyBaking(net.minecraftforge.client.event.ModelEvent.ModifyBakingResult event) {
        net.minecraft.client.resources.model.ModelResourceLocation location =
                new net.minecraft.client.resources.model.ModelResourceLocation(
                        new net.minecraft.resources.ResourceLocation(FishingGuiMod.MOD_ID, "tackle_rod"), "inventory");
        net.minecraft.client.resources.model.BakedModel base = event.getModels().get(location);
        if (base != null) {
            event.getModels().put(location, new TackleRodModel(base));
        }
    }

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("level_up",
                (gui, graphics, partialTick, width, height) -> LevelUpOverlay.render(graphics, width, height));
    }
}
