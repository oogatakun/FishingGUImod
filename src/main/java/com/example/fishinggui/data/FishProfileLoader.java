package com.example.fishinggui.data;

import com.example.fishinggui.FishingGuiMod;
import com.example.fishinggui.fight.FishInfo;
import com.example.fishinggui.fight.FishProfile;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

/** Loads per-item fish profiles from datapacks (data/&lt;ns&gt;/fishinggui/fish_profiles). Server side only. */
@Mod.EventBusSubscriber(modid = FishingGuiMod.MOD_ID)
public final class FishProfileLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();
    private static volatile Map<ResourceLocation, FishProfile> profiles = Map.of();
    private static volatile Map<ResourceLocation, FishInfo> infos = Map.of();

    private FishProfileLoader() {
        super(GSON, "fishinggui/fish_profiles");
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new FishProfileLoader());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, FishProfile> loaded = new HashMap<>();
        Map<ResourceLocation, FishInfo> loadedInfo = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> file : new TreeMap<>(files).entrySet()) {
            try {
                FishProfileParser.Entry entry = FishProfileParser.parse(file.getValue().getAsJsonObject());
                ResourceLocation item = ResourceLocation.tryParse(entry.item());
                if (item == null) {
                    throw new IllegalArgumentException("invalid item id '" + entry.item() + "'");
                }
                loaded.put(item, entry.profile());
                loadedInfo.put(item, entry.info());
            } catch (RuntimeException e) {
                LOGGER.warn("Skipping fish profile {}: {}", file.getKey(), e.getMessage());
            }
        }
        profiles = Map.copyOf(loaded);
        infos = Map.copyOf(loadedInfo);
        LOGGER.info("Loaded {} fish profiles", loaded.size());
    }

    public static FishProfile profileFor(List<ItemStack> loot) {
        if (loot.isEmpty()) {
            return FishProfile.DEFAULT;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(loot.get(0).getItem());
        return profiles.getOrDefault(id, FishProfile.DEFAULT);
    }

    public static FishInfo infoFor(List<ItemStack> loot) {
        if (loot.isEmpty()) {
            return FishInfo.DEFAULT;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(loot.get(0).getItem());
        return infos.getOrDefault(id, FishInfo.DEFAULT);
    }
}
