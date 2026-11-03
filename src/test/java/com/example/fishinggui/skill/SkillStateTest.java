package com.example.fishinggui.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SkillStateTest {
    private static final SkillDef DEF = new SkillDef("x", 3, List.of(1, 2, 3));

    @Test
    void startIsEmpty() {
        assertEquals(0, SkillState.START.points());
        assertEquals(0, SkillState.START.levelOf("x"));
    }

    @Test
    void addPoints() {
        assertEquals(3, SkillState.START.addPoints(3).points());
    }

    @Test
    void negativePointsClampToZero() {
        assertEquals(0, SkillState.START.addPoints(2).addPoints(-5).points());
        assertEquals(0, SkillState.START.withPoints(-1).points());
    }

    @Test
    void buySpendsCostAndLevelsUp() {
        SkillState s = SkillState.START.addPoints(10);
        SkillState one = s.buy(DEF);
        assertEquals(1, one.levelOf("x"));
        assertEquals(9, one.points());
        SkillState two = one.buy(DEF);
        assertEquals(2, two.levelOf("x"));
        assertEquals(7, two.points());
        assertEquals(0, s.levelOf("x"));
        assertEquals(10, s.points());
    }

    @Test
    void insufficientPointsRejected() {
        SkillState s = SkillState.START.addPoints(1).buy(DEF);
        assertEquals(0, s.points());
        assertFalse(s.canBuy(DEF));
        assertThrows(IllegalStateException.class, () -> s.buy(DEF));
    }

    @Test
    void maxLevelRejected() {
        SkillState s = new SkillState(100, Map.of("x", 3));
        assertFalse(s.canBuy(DEF));
        assertThrows(IllegalStateException.class, () -> s.buy(DEF));
        assertEquals(-1, DEF.nextCost(3));
        assertEquals(2, DEF.nextCost(1));
        assertTrue(SkillState.START.addPoints(1).canBuy(DEF));
    }
}
