package com.example.fishinggui.item;

import com.example.fishinggui.FishingGuiMod;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import com.example.fishinggui.food.FoodRules;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
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

    /** Grilled fish by raw fish id (FishIds). Nutrition/saturation come from the level; heal is applied on eating. */
    public static final Map<String, RegistryObject<Item>> COOKED = registerCooked();

    public static final RegistryObject<Item> BURNT_FISH = ITEMS.register("burnt_fish", () -> new Item(
            new Item.Properties().food(new FoodProperties.Builder().nutrition(1).saturationMod(0.1f)
                    .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 600, 1), 1.0f).build())));

    /** Cut up on a Farmer's Delight cutting board; used by the seafood hotpot. */
    public static final RegistryObject<Item> FISH_FILLET =
            ITEMS.register("fish_fillet", () -> new Item(new Item.Properties()));

    /** The pot, placed as a block; right-click it with a bowl to serve SEAFOOD_HOTPOT_BOWL. */
    public static final RegistryObject<Item> SEAFOOD_HOTPOT = ITEMS.register("seafood_hotpot",
            () -> new HotpotItem(com.example.fishinggui.block.ModBlocks.SEAFOOD_HOTPOT.get(),
                    new Item.Properties().stacksTo(16)));

    /** One bowl of hotpot; also grants Farmer's Delight's Nourishment for 5 minutes when FD is installed. */
    public static final RegistryObject<Item> SEAFOOD_HOTPOT_BOWL = ITEMS.register("seafood_hotpot_bowl",
            () -> new HotpotBowlItem(new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
                    .nutrition(8).saturationMod(0.8f).build())));

    private static Map<String, RegistryObject<Item>> registerCooked() {
        Map<String, RegistryObject<Item>> map = new LinkedHashMap<>();
        for (String id : FishIds.ALL) {
            int level = FishLevels.BY_ID.get(FishingGuiMod.MOD_ID + ":" + id);
            FoodProperties food = new FoodProperties.Builder()
                    .nutrition(FoodRules.nutrition(level))
                    .saturationMod(FoodRules.saturationModifier(level))
                    .build();
            map.put(id, ITEMS.register("cooked_" + id,
                    () -> new CookedFishItem(new Item.Properties().food(food), id)));
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
                for (String id : FishIds.ALL) {
                    output.accept(FISH.get(id).get());
                    output.accept(COOKED.get(id).get());
                }
                output.accept(BURNT_FISH.get());
                output.accept(com.example.fishinggui.block.ModBlocks.SHICHIRIN_ITEM.get());
            })
            .build());

    /** Items that only matter together with Farmer's Delight. */
    public static final RegistryObject<CreativeModeTab> FD_TAB = TABS.register("farmers_delight",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.fishinggui.farmers_delight"))
                    .icon(() -> new ItemStack(SEAFOOD_HOTPOT.get()))
                    .withTabsBefore(TAB.getId())
                    .displayItems((params, output) -> {
                        output.accept(FISH_FILLET.get());
                        output.accept(SEAFOOD_HOTPOT.get());
                        output.accept(SEAFOOD_HOTPOT_BOWL.get());
                    })
                    .build());

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        TABS.register(modBus);
    }
}
