package com.example.fishinggui.item;

import com.example.fishinggui.FishingGuiMod;
import com.example.fishinggui.fight.BaitType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, FishingGuiMod.MOD_ID);

    public static final RegistryObject<Item> WORM = ITEMS.register("worm", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GLOW_BUG =
            ITEMS.register("glow_bug", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STURDY_PASTE =
            ITEMS.register("sturdy_paste", () -> new Item(new Item.Properties()));

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(ModItems::addToCreativeTab);
    }

    public static BaitType typeOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return BaitType.NONE;
        }
        if (stack.is(WORM.get())) {
            return BaitType.WORM;
        }
        if (stack.is(GLOW_BUG.get())) {
            return BaitType.GLOW_BUG;
        }
        if (stack.is(STURDY_PASTE.get())) {
            return BaitType.STURDY_PASTE;
        }
        return BaitType.NONE;
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(WORM);
            event.accept(GLOW_BUG);
            event.accept(STURDY_PASTE);
        }
    }
}
