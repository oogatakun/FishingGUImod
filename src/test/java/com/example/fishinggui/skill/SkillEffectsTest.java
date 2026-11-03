package com.example.fishinggui.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SkillEffectsTest {
    @Test
    void biteSpeedBonus() {
        assertEquals(0.0, SkillEffects.biteSpeedBonus(0), 1e-9);
        assertEquals(0.45, SkillEffects.biteSpeedBonus(3), 1e-9);
        assertEquals(0.75, SkillEffects.biteSpeedBonus(5), 1e-9);
        assertEquals(0.75, SkillEffects.biteSpeedBonus(9), 1e-9);
        assertEquals(0.0, SkillEffects.biteSpeedBonus(-1), 1e-9);
    }

    @Test
    void biteSpeedExtraTicksMatchesReduction() {
        assertEquals(0.0, SkillEffects.biteSpeedExtraTicks(0), 1e-9);
        assertEquals(3.0, SkillEffects.biteSpeedExtraTicks(5), 1e-9);
    }
}
