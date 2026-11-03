package com.example.fishinggui.data;

import com.example.fishinggui.FishingGuiMod;
import com.example.fishinggui.level.LevelCurve;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/** Loads the XP curve from datapacks (data/&lt;ns&gt;/fishinggui/fishing_levels). Server side only. */
@Mod.EventBusSubscriber(modid = FishingGuiMod.MOD_ID)
public final class LevelCurveLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();
    private static volatile LevelCurve curve = LevelCurve.DEFAULT;

    private LevelCurveLoader() {
        super(GSON, "fishinggui/fishing_levels");
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new LevelCurveLoader());
    }

    public static LevelCurve curve() {
        return curve;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        LevelCurve loaded = LevelCurve.DEFAULT;
        for (Map.Entry<ResourceLocation, JsonElement> file : new TreeMap<>(files).entrySet()) {
            try {
                loaded = LevelCurveParser.parse(file.getValue().getAsJsonObject());
            } catch (RuntimeException e) {
                LOGGER.warn("Skipping fishing level file {}: {}", file.getKey(), e.getMessage());
            }
        }
        curve = loaded;
        LOGGER.info("Loaded fishing level curve");
    }
}
