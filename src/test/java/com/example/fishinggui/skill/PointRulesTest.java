package com.example.fishinggui.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.fishinggui.fight.Rarity;
import org.junit.jupiter.api.Test;

class PointRulesTest {
    @Test
    void pointsByRarity() {
        assertEquals(1, PointRules.pointsFor(Rarity.COMMON));
        assertEquals(2, PointRules.pointsFor(Rarity.UNCOMMON));
        assertEquals(3, PointRules.pointsFor(Rarity.RARE));
        assertEquals(4, PointRules.pointsFor(Rarity.EPIC));
    }
}
