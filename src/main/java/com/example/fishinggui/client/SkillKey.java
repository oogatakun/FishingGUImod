package com.example.fishinggui.client;

import com.example.fishinggui.FishingGuiMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** The J key that opens the skill screen. */
public final class SkillKey {
    public static final KeyMapping OPEN = new KeyMapping("key.fishinggui.skills", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J, "key.categories.fishinggui");

    private SkillKey() {
    }

    @Mod.EventBusSubscriber(modid = FishingGuiMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Register {
        @SubscribeEvent
        public static void onRegister(RegisterKeyMappingsEvent event) {
            event.register(OPEN);
        }
    }

    @Mod.EventBusSubscriber(modid = FishingGuiMod.MOD_ID, value = Dist.CLIENT)
    public static final class Tick {
        @SubscribeEvent
        public static void onTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            while (OPEN.consumeClick()) {
                if (mc.screen == null && mc.player != null) {
                    mc.setScreen(new SkillScreen());
                }
            }
        }
    }
}
