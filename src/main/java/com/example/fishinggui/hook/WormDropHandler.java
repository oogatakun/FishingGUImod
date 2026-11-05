package com.example.fishinggui.hook;

import com.example.fishinggui.FishingGuiMod;
import com.example.fishinggui.item.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Breaking a grass block now and then turns up a worm (not in creative, not with Silk Touch). */
@Mod.EventBusSubscriber(modid = FishingGuiMod.MOD_ID)
public final class WormDropHandler {
    public static final double WORM_CHANCE = 0.1;

    private WormDropHandler() {
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getPlayer().isCreative()
                || !event.getState().is(Blocks.GRASS_BLOCK)
                || EnchantmentHelper.getTagEnchantmentLevel(Enchantments.SILK_TOUCH,
                        event.getPlayer().getMainHandItem()) > 0) {
            return;
        }
        if (level.getRandom().nextDouble() < WORM_CHANCE) {
            Block.popResource(level, event.getPos(), new ItemStack(ModItems.WORM.get()));
        }
    }
}
