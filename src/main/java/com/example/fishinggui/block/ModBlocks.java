package com.example.fishinggui.block;

import com.example.fishinggui.FishingGuiMod;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

public final class ModBlocks {
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, FishingGuiMod.MOD_ID);
    private static final DeferredRegister<Item> BLOCK_ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, FishingGuiMod.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, FishingGuiMod.MOD_ID);

    public static final RegistryObject<Block> SHICHIRIN = BLOCKS.register("shichirin",
            () -> new ShichirinBlock(Block.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE)
                    .strength(1.0f).sound(SoundType.STONE).noOcclusion()
                    .lightLevel(s -> !s.getValue(ShichirinBlock.LIT) ? 0 : s.getValue(ShichirinBlock.WEAK) ? 7 : 13)));

    public static final RegistryObject<Block> SEAFOOD_HOTPOT = BLOCKS.register("seafood_hotpot",
            () -> new SeafoodHotpotBlock(Block.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE)
                    .strength(0.5f).sound(SoundType.STONE).noOcclusion()));

    public static final RegistryObject<Block> TAKOYAKI_PAN = BLOCKS.register("takoyaki_pan",
            () -> new TakoyakiPanBlock(Block.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(1.5f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(s -> s.getValue(TakoyakiPanBlock.POWERED) ? 6 : 0)));

    public static final RegistryObject<Item> TAKOYAKI_PAN_ITEM = BLOCK_ITEMS.register("takoyaki_pan",
            () -> TakoyakiPanBlock.item(TAKOYAKI_PAN.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<TakoyakiPanBlockEntity>> TAKOYAKI_PAN_BE =
            BLOCK_ENTITIES.register("takoyaki_pan", () ->
                    BlockEntityType.Builder.of(TakoyakiPanBlockEntity::new, TAKOYAKI_PAN.get()).build(null));

    public static final RegistryObject<Item> SHICHIRIN_ITEM = BLOCK_ITEMS.register("shichirin",
            () -> new BlockItem(SHICHIRIN.get(), new Item.Properties()) {
                @Override
                public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                        TooltipFlag flag) {
                    tooltip.add(Component.translatable("tooltip.fishinggui.shichirin"));
                }
            });

    public static final RegistryObject<BlockEntityType<ShichirinBlockEntity>> SHICHIRIN_BE =
            BLOCK_ENTITIES.register("shichirin", () ->
                    BlockEntityType.Builder.of(ShichirinBlockEntity::new, SHICHIRIN.get()).build(null));

    private ModBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }
}
