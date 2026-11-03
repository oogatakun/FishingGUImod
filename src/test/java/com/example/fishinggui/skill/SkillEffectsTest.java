package com.example.fishinggui.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SkillEffectsTest {
    @Test
    void biteSpeedBonus() {
        assertEquals(0.0, SkillEffects.biteSpeedBonus(0), 1e-9);
        assertEquals(0.3, SkillEffects.biteSpeedBonus(3), 1e-9);
        assertEquals(0.5, SkillEffects.biteSpeedBonus(5), 1e-9);
        assertEquals(0.5, SkillEffects.biteSpeedBonus(9), 1e-9);
        assertEquals(0.0, SkillEffects.biteSpeedBonus(-1), 1e-9);
    }
}
