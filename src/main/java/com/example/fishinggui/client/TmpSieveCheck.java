package com.example.fishinggui.client;

import org.slf4j.Logger;

import com.example.fishinggui.FishingGuiMod;
import com.example.fishinggui.sieve.SieveItem;
import com.example.fishinggui.sieve.SieveItems;
import com.example.fishinggui.sieve.SieveOre;
import com.mojang.logging.LogUtils;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** TEMPORARY in-game check of the sieve after moving it into this mod. Delete before committing. */
@Mod.EventBusSubscriber(modid = FishingGuiMod.MOD_ID, value = Dist.CLIENT)
public final class TmpSieveCheck {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean titleSeen;
    private static boolean worldRequested;
    private static int tick;

    private TmpSieveCheck() {
    }

    @SubscribeEvent
    public static void onScreen(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof TitleScreen) {
            titleSeen = true;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!worldRequested && titleSeen && mc.screen instanceof TitleScreen) {
            worldRequested = true;
            LevelSettings settings = new LevelSettings("SieveCheck", GameType.CREATIVE, false, Difficulty.PEACEFUL,
                    true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("sievecheck", settings, new WorldOptions(5L, false, false),
                    access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                            .value().createWorldDimensions());
            return;
        }
        if (mc.player == null || mc.level == null) {
            return;
        }
        tick++;
        try {
            step(mc);
        } catch (Exception e) {
            LOGGER.error("[sievecheck] failed", e);
            mc.stop();
        }
    }

    private static void step(Minecraft mc) throws Exception {
        if (tick == 40) {
            for (String c : new String[] {"gamerule sendCommandFeedback false", "gamerule doDaylightCycle false",
                    "time set 6000", "tp @s 0.5 -60 0.5 0 20"}) {
                mc.player.connection.sendCommand(c);
            }
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            StringBuilder order = new StringBuilder();
            for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
                ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
                order.append(id).append(' ');
            }
            LOGGER.info("[sievecheck] tab order: {}", order);
        }
        if (tick == 50) {
            mc.setScreen(new CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), true));
        }
        if (tick == 60 && mc.screen instanceof CreativeModeInventoryScreen screen) {
            CreativeModeTab tab = SieveItems.TAB.get();
            var select = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
            select.setAccessible(true);
            select.invoke(screen, tab);
            LOGGER.info("[sievecheck] tab items: {}", tab.getDisplayItems().size());
        }
        if (tick == 70) {
            Screenshot.grab(mc.gameDirectory, "sievecheck_tab.png", mc.getMainRenderTarget(), m -> {
            });
        }
        if (tick == 75) {
            mc.setScreen(null);
            for (String c : new String[] {"gamemode survival", "clear @s", "give @s fishinggui:sieve",
                    "item replace entity @s weapon.offhand with minecraft:gravel 64"}) {
                mc.player.connection.sendCommand(c);
            }
        }
        if (tick == 85) {
            mc.player.getInventory().selected = 0;
        }
        mc.options.keyUse.setDown(tick >= 90 && tick < 90 + 8 * 30 + 5 && mc.screen == null);
        if (tick == 90 + 15) {
            Screenshot.grab(mc.gameDirectory, "sievecheck_sift.png", mc.getMainRenderTarget(), m -> {
            });
        }
        if (tick == 360) {
            report(mc.getSingleplayerServer());
            Screenshot.grab(mc.gameDirectory, "sievecheck_after.png", mc.getMainRenderTarget(), m -> {
            });
        }
        if (tick == 370) {
            mc.stop();
        }
    }

    private static void report(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().get(0);
        LOGGER.info("[sievecheck] main {} damage {} offhand {} x{}", player.getMainHandItem().getItem(),
                player.getMainHandItem().getDamageValue(), player.getOffhandItem().getItem(),
                player.getOffhandItem().getCount());
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.isEmpty()) {
                LOGGER.info("[sievecheck] slot {}: {} x{}", i, ForgeRegistries.ITEMS.getKey(s.getItem()), s.getCount());
            }
        }
        long sieveRecipes = server.getRecipeManager().getRecipeIds()
                .filter(id -> id.getNamespace().equals(FishingGuiMod.MOD_ID) && id.getPath().startsWith("sieve/"))
                .count();
        LOGGER.info("[sievecheck] sieve recipes loaded {} (want 17); dirt/sand/red_sand/gravel siftable {} {} {} {}",
                sieveRecipes, new ItemStack(Items.DIRT).is(SieveItem.SIFTABLE),
                new ItemStack(Items.SAND).is(SieveItem.SIFTABLE), new ItemStack(Items.RED_SAND).is(SieveItem.SIFTABLE),
                new ItemStack(Items.GRAVEL).is(SieveItem.SIFTABLE));
        server.getRecipeManager().byKey(new ResourceLocation(FishingGuiMod.MOD_ID, "sieve/sieve")).ifPresent(r -> LOGGER
                .info("[sievecheck] sieve recipe -> {}", r.getResultItem(server.registryAccess()).getItem()));
        server.getRecipeManager().byKey(new ResourceLocation(FishingGuiMod.MOD_ID, "sieve/raw_iron_from_iron_ore_shards"))
                .ifPresent(r -> LOGGER.info("[sievecheck] iron shards -> {}",
                        r.getResultItem(server.registryAccess()).getItem()));
        LOGGER.info("[sievecheck] bit item {} ", SieveItems.bit(SieveOre.DIAMOND));
    }
}
