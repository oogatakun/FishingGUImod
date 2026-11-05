package com.example.fishinggui.food;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TakoyakiRulesTest {
    @Test
    void cookingStagesAdvanceToTheNextManualStep() {
        assertEquals(TakoyakiRules.FLIP, TakoyakiRules.afterCooking(TakoyakiRules.COOKING));
        assertEquals(TakoyakiRules.READY, TakoyakiRules.afterCooking(TakoyakiRules.COOKING_BACK));
    }

    @Test
    void manualStagesDoNotAdvanceOnTheirOwn() {
        for (int stage : new int[] {TakoyakiRules.EMPTY, TakoyakiRules.BATTER, TakoyakiRules.FILLED,
                TakoyakiRules.FLIP, TakoyakiRules.READY}) {
            assertEquals(stage, TakoyakiRules.afterCooking(stage));
            assertFalse(TakoyakiRules.isCooking(stage));
        }
        assertTrue(TakoyakiRules.isCooking(TakoyakiRules.COOKING));
        assertTrue(TakoyakiRules.isCooking(TakoyakiRules.COOKING_BACK));
    }

    @Test
    void twoScoopsEmptyThePan() {
        assertEquals(TakoyakiRules.WELLS, 2 * TakoyakiRules.PORTIONS);
    }
}
