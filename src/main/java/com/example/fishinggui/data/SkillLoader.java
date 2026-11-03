package com.example.fishinggui.data;

import com.example.fishinggui.FishingGuiMod;
import com.example.fishinggui.skill.SkillDef;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.LinkedHashMap;
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

/** Loads skill definitions from datapacks (data/&lt;ns&gt;/fishinggui/skills). Server side only. */
@Mod.EventBusSubscriber(modid = FishingGuiMod.MOD_ID)
public final class SkillLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();
    private static volatile Map<String, SkillDef> skills = Map.of();

    private SkillLoader() {
        super(GSON, "fishinggui/skills");
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new SkillLoader());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, SkillDef> loaded = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> file : new TreeMap<>(files).entrySet()) {
            try {
                String id = file.getKey().getPath();
                loaded.put(id, SkillParser.parse(id, file.getValue().getAsJsonObject()));
            } catch (RuntimeException e) {
                LOGGER.warn("Skipping skill {}: {}", file.getKey(), e.getMessage());
            }
        }
        skills = Collections.unmodifiableMap(loaded);
        LOGGER.info("Loaded {} skills", loaded.size());
    }

    public static Map<String, SkillDef> skills() {
        return skills;
    }
}
