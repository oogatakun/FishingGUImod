package com.example.fishinggui.item;

import com.example.fishinggui.FishingGuiMod;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, FishingGuiMod.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FishingGuiMod.MOD_ID);

    public static final RegistryObject<Item> WORM = ITEMS.register("worm", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> RESET_DROP =
            ITEMS.register("reset_drop", () -> new Item(new Item.Properties()));

    public static final Map<String, RegistryObject<Item>> FISH = registerFish();

    private static Map<String, RegistryObject<Item>> registerFish() {
        Map<String, RegistryObject<Item>> map = new LinkedHashMap<>();
        for (String id : FishIds.ALL) {
            map.put(id, ITEMS.register(id, () -> new Item(new Item.Properties())));
        }
        return map;
    }

    private ModItems() {
    }

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.fishinggui"))
            .icon(() -> new ItemStack(Items.FISHING_ROD))
            .displayItems((params, output) -> {
                output.accept(WORM.get());
                output.accept(RESET_DROP.get());
                FISH.values().forEach(r -> output.accept(r.get()));
            })
            .build());

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        TABS.register(modBus);
    }
}
