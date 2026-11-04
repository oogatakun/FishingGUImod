package com.example.fishinggui.food;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.fishinggui.fight.Rarity;
import org.junit.jupiter.api.Test;

class FoodRulesTest {
    @Test
    void nutritionByLevel() {
        assertEquals(3, FoodRules.nutrition(1));
        assertEquals(3, FoodRules.nutrition(4));
        assertEquals(4, FoodRules.nutrition(5));
        assertEquals(5, FoodRules.nutrition(10));
        assertEquals(8, FoodRules.nutrition(20));
    }

    @Test
    void saturationModifierByLevel() {
        assertEquals(0.6f, FoodRules.saturationModifier(1), 1e-6f);
        assertEquals(0.7f, FoodRules.saturationModifier(11), 1e-6f);
        assertEquals(0.79f, FoodRules.saturationModifier(20), 1e-6f);
    }

    @Test
    void healByRarity() {
        assertEquals(0, FoodRules.heal(Rarity.COMMON));
        assertEquals(1, FoodRules.heal(Rarity.UNCOMMON));
        assertEquals(2, FoodRules.heal(Rarity.RARE));
        assertEquals(4, FoodRules.heal(Rarity.EPIC));
    }
}
