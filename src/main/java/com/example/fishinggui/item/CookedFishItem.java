package com.example.fishinggui.item;

import com.example.fishinggui.FishingGuiMod;
import com.example.fishinggui.data.FishProfileLoader;
import com.example.fishinggui.fight.FishInfo;
import com.example.fishinggui.food.FoodRules;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Grilled fish. The instant heal depends on datapack data (rarity, per-fish food override) that is not
 * available when items are registered, so it is looked up here when the food is eaten.
 */
public class CookedFishItem extends Item {
    private final String rawId;

    public CookedFishItem(Properties properties, String rawId) {
        super(properties);
        this.rawId = rawId;
    }

    /** Heal in half-hearts for the raw fish this was grilled from, from the loaded datapack data. */
    public static int healFor(String rawId) {
        FishInfo info = FishProfileLoader.infoFor(new ResourceLocation(FishingGuiMod.MOD_ID, rawId));
        return info.foodOverride().map(o -> o.heal()).orElseGet(() -> FoodRules.heal(info.rarity()));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide && entity instanceof Player player) {
            int heal = healFor(rawId);
            if (heal > 0) {
                player.heal(heal);
            }
        }
        return result;
    }
}
