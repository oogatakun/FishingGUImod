package com.example.fishinggui.sieve;

import java.util.EnumMap;
import java.util.Map;

import com.example.fishinggui.FishingGuiMod;
import com.example.fishinggui.item.ModItems;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * The sieve and what it turns up: sift dirt, sand and gravel for ore bits; nine bits make a shard, nine shards
 * make the raw ore (or the item itself). They have a creative tab of their own, after the fishing tabs.
 */
public final class SieveItems {
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, FishingGuiMod.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FishingGuiMod.MOD_ID);

    public static final RegistryObject<Item> SIEVE =
            ITEMS.register("sieve", () -> new SieveItem(new Item.Properties().durability(256)));

    private static final Map<SieveOre, RegistryObject<Item>> BITS = new EnumMap<>(SieveOre.class);
    private static final Map<SieveOre, RegistryObject<Item>> SHARDS = new EnumMap<>(SieveOre.class);

    static {
        for (SieveOre ore : SieveOre.values()) {
            BITS.put(ore, ITEMS.register(ore.id + "_ore_bit", () -> new Item(new Item.Properties())));
            SHARDS.put(ore, ITEMS.register(ore.id + "_ore_shard", () -> new Item(new Item.Properties())));
        }
    }

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("sieve", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.fishinggui.sieve"))
            .icon(() -> new ItemStack(SIEVE.get()))
            .withTabsBefore(ModItems.FD_TAB.getId())
            .displayItems((params, output) -> {
                output.accept(SIEVE.get());
                for (SieveOre ore : SieveOre.values()) {
                    output.accept(bit(ore));
                }
                for (SieveOre ore : SieveOre.values()) {
                    output.accept(shard(ore));
                }
            })
            .build());

    private SieveItems() {
    }

    public static Item bit(SieveOre ore) {
        return BITS.get(ore).get();
    }

    public static Item shard(SieveOre ore) {
        return SHARDS.get(ore).get();
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        TABS.register(modBus);
    }
}
