package com.example.fishinggui.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A boat of takoyaki: each bite eats one piece; the damage bar shows how many are left. */
public class TakoyakiItem extends Item {
    public TakoyakiItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return super.finishUsingItem(stack, level, entity);
        }
        player.eat(level, stack.copy());
        if (player.getAbilities().instabuild) {
            return stack;
        }
        if (stack.getDamageValue() + 1 >= stack.getMaxDamage()) {
            return ItemStack.EMPTY;
        }
        stack.setDamageValue(stack.getDamageValue() + 1);
        return stack;
    }
}
