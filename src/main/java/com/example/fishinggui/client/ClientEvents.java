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
        event.registerBlockEntityRenderer(com.example.fishinggui.block.ModBlocks.SHICHIRIN_BE.get(),
                ShichirinRenderer::new);
        event.registerBlockEntityRenderer(com.example.fishinggui.block.ModBlocks.TAKOYAKI_PAN_BE.get(),
                TakoyakiPanRenderer::new);
    }

    @SubscribeEvent
    public static void onClientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            net.minecraft.client.gui.screens.MenuScreens.register(
                    com.example.fishinggui.block.ModMenus.TACKLE_BENCH.get(), TackleBenchScreen::new);
            // the casting sprite, the same rule as the vanilla rod
            net.minecraft.client.renderer.item.ItemProperties.register(
                    com.example.fishinggui.item.ModItems.TACKLE_ROD.get(),
                    new net.minecraft.resources.ResourceLocation("cast"), (stack, level, entity, seed) -> {
                        if (!(entity instanceof net.minecraft.world.entity.player.Player player)) {
                            return 0.0f;
                        }
                        boolean main = player.getMainHandItem() == stack;
                        boolean off = player.getOffhandItem() == stack
                                && !(player.getMainHandItem().getItem()
                                        instanceof net.minecraft.world.item.FishingRodItem);
                        return (main || off) && player.fishing != null ? 1.0f : 0.0f;
                    });
        });
    }

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("level_up",
                (gui, graphics, partialTick, width, height) -> LevelUpOverlay.render(graphics, width, height));
    }
}
