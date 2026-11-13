package com.example.fishinggui.entity;

import com.example.fishinggui.FishingGuiMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = FishingGuiMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FishingGuiMod.MOD_ID);

    /** Never spawns by itself (MISC): it only comes from the worm item and from breeding. */
    public static final RegistryObject<EntityType<WormEntity>> WORM = ENTITIES.register("worm",
            () -> EntityType.Builder.of(WormEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.12F).clientTrackingRange(8).updateInterval(3)
                    .build(FishingGuiMod.MOD_ID + ":worm"));

    private ModEntities() {
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(WORM.get(), WormEntity.attributes().build());
    }
}
