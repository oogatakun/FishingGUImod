package com.example.fishinggui.item;

import com.example.fishinggui.FishingGuiMod;
import java.util.LinkedHashMap;
import java.util.List;
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

    public static final RegistryObject<Item> WORM = ITEMS.register("worm", () -> new WormItem(new Item.Properties()));

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
            ITEMS.register("fish_fillet", () -> new Item(new Item.Properties().food(
                    new FoodProperties.Builder().nutrition(1).saturationMod(0.3f).fast().build())));

    /** The fillet of each fish, by raw fish id; cut from the fish on a Farmer's Delight cutting board. */
    public static final Map<String, RegistryObject<Item>> FILLETS = registerFillets();

    private static Map<String, RegistryObject<Item>> registerFillets() {
        Map<String, RegistryObject<Item>> map = new LinkedHashMap<>();
        for (String id : FishIds.ALL) {
            map.put(id, ITEMS.register(id + "_fillet", () -> new Item(new Item.Properties().food(
                    new FoodProperties.Builder().nutrition(1).saturationMod(0.3f).fast().build()))));
        }
        return map;
    }

    /** The pot, placed as a block; right-click it with a bowl to serve SEAFOOD_HOTPOT_BOWL. */
    public static final RegistryObject<Item> SEAFOOD_HOTPOT = ITEMS.register("seafood_hotpot",
            () -> new HotpotItem(com.example.fishinggui.block.ModBlocks.SEAFOOD_HOTPOT.get(),
                    new Item.Properties().stacksTo(16), false));

    /** The emptied pot: placing it gives an empty pot again. */
    public static final RegistryObject<Item> EMPTY_SEAFOOD_HOTPOT = ITEMS.register("empty_seafood_hotpot",
            () -> new HotpotItem(com.example.fishinggui.block.ModBlocks.SEAFOOD_HOTPOT.get(),
                    new Item.Properties().stacksTo(16), true));

    /** One bowl of hotpot; also grants Farmer's Delight's Nourishment for 5 minutes when FD is installed. */
    public static final RegistryObject<Item> SEAFOOD_HOTPOT_BOWL = ITEMS.register("seafood_hotpot_bowl",
            () -> new HotpotBowlItem(new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
                    .nutrition(8).saturationMod(0.8f).build())));

    public static final RegistryObject<Item> OCTOPUS = ITEMS.register("octopus", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CUT_OCTOPUS =
            ITEMS.register("cut_octopus", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> TAKOYAKI_BATTER =
            ITEMS.register("takoyaki_batter", () -> new Item(new Item.Properties().durability(24)));

    public static final RegistryObject<Item> ICE_PICK =
            ITEMS.register("ice_pick", () -> new Item(new Item.Properties().stacksTo(1).durability(256)));

    public static final RegistryObject<Item> MAYONNAISE =
            ITEMS.register("mayonnaise", () -> new Item(new Item.Properties().durability(6)));

    public static final RegistryObject<Item> AONORI = ITEMS.register("aonori", () -> new Item(new Item.Properties()));

    /** Six pieces on a boat; each bite eats one (see TakoyakiItem). */
    public static final RegistryObject<Item> TAKOYAKI = ITEMS.register("takoyaki",
            () -> new TakoyakiItem(com.example.fishinggui.block.ModBlocks.TAKOYAKI_BOAT.get(),
                    new Item.Properties().durability(6).food(new FoodProperties.Builder()
                    .nutrition(4).saturationMod(0.7f).build())));

    /** Rod parts that can be built today, by part and rank (index 0 is rank 1). */
    public static final Map<com.example.fishinggui.tackle.Part, List<RegistryObject<Item>>> PARTS =
            registerParts();

    /** A rod assembled at a tackle bench. */
    public static final RegistryObject<Item> TACKLE_ROD = ITEMS.register("tackle_rod",
            () -> new TackleRodItem(new Item.Properties().stacksTo(1).durability(128)));

    private static Map<com.example.fishinggui.tackle.Part, List<RegistryObject<Item>>> registerParts() {
        Map<com.example.fishinggui.tackle.Part, List<RegistryObject<Item>>> map =
                new java.util.EnumMap<>(com.example.fishinggui.tackle.Part.class);
        for (com.example.fishinggui.tackle.Part part : com.example.fishinggui.tackle.Part.values()) {
            if (!part.available()) {
                continue;
            }
            List<RegistryObject<Item>> ranks = new java.util.ArrayList<>();
            for (int rank = 1; rank <= com.example.fishinggui.tackle.Part.MAX_RANK; rank++) {
                int r = rank;
                ranks.add(ITEMS.register(part.itemName(rank), () -> partItem(part, r, null)));
            }
            map.put(part, List.copyOf(ranks));
        }
        return map;
    }

    private static Item partItem(com.example.fishinggui.tackle.Part part, int rank,
            com.example.fishinggui.tackle.Style style) {
        if (part == com.example.fishinggui.tackle.Part.LURE) {
            return new LureItem(rank, style, new Item.Properties().stacksTo(16));
        }
        Item.Properties properties = part == com.example.fishinggui.tackle.Part.ROD_BLANK
                ? new Item.Properties().stacksTo(1).durability(
                        com.example.fishinggui.tackle.RodSetup.durabilityOf(rank, style))
                : new Item.Properties().stacksTo(16);
        if (part == com.example.fishinggui.tackle.Part.LINE) {
            // a line wears with every catch and snaps when it is used up
            properties.durability(com.example.fishinggui.tackle.RodSetup.lineLife(rank));
        }
        return new TacklePartItem(part, rank, style, properties);
    }

    /** The parts in a fishing style, by part and style. */
    public static final Map<com.example.fishinggui.tackle.Part, Map<com.example.fishinggui.tackle.Style,
            RegistryObject<Item>>> STYLED = registerStyled();

    private static Map<com.example.fishinggui.tackle.Part, Map<com.example.fishinggui.tackle.Style,
            RegistryObject<Item>>> registerStyled() {
        Map<com.example.fishinggui.tackle.Part, Map<com.example.fishinggui.tackle.Style, RegistryObject<Item>>> map =
                new java.util.EnumMap<>(com.example.fishinggui.tackle.Part.class);
        for (com.example.fishinggui.tackle.Part part : com.example.fishinggui.tackle.Part.values()) {
            Map<com.example.fishinggui.tackle.Style, RegistryObject<Item>> styles =
                    new java.util.EnumMap<>(com.example.fishinggui.tackle.Style.class);
            for (com.example.fishinggui.tackle.Style style : com.example.fishinggui.tackle.Style.values()) {
                styles.put(style, ITEMS.register(part.itemName(style.rank(), style),
                        () -> partItem(part, style.rank(), style)));
            }
            map.put(part, styles);
        }
        return map;
    }

    /** The item of a part at a rank, in a style or (null) plain. */
    public static Item part(com.example.fishinggui.tackle.Part part, int rank,
            com.example.fishinggui.tackle.Style style) {
        return style == null ? PARTS.get(part).get(rank - 1).get() : STYLED.get(part).get(style).get();
    }

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
                output.accept(OCTOPUS.get());
                output.accept(com.example.fishinggui.block.ModBlocks.TACKLE_BENCH_ITEM.get());
                output.accept(TACKLE_ROD.get());
                PARTS.values().forEach(l -> l.forEach(p -> output.accept(p.get())));
                STYLED.values().forEach(m -> m.values().forEach(p -> output.accept(p.get())));
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
                        for (String id : FishIds.ALL) {
                            output.accept(FILLETS.get(id).get());
                        }
                        output.accept(SEAFOOD_HOTPOT.get());
                        output.accept(EMPTY_SEAFOOD_HOTPOT.get());
                        output.accept(SEAFOOD_HOTPOT_BOWL.get());
                        output.accept(com.example.fishinggui.block.ModBlocks.TAKOYAKI_PAN_ITEM.get());
                        output.accept(CUT_OCTOPUS.get());
                        output.accept(TAKOYAKI_BATTER.get());
                        output.accept(ICE_PICK.get());
                        output.accept(MAYONNAISE.get());
                        output.accept(AONORI.get());
                        output.accept(TAKOYAKI.get());
                    })
                    .build());

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        TABS.register(modBus);
    }
}
