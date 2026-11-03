package com.example.fishinggui.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.fishinggui.fight.FishProfile;
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

    @Test
    void fightSkillScales() {
        assertEquals(0.7, SkillEffects.dashEaseScale(5), 1e-9);
        assertEquals(1.0, SkillEffects.dashEaseScale(0), 1e-9);
        assertEquals(1.4, SkillEffects.dashDelayScale(5), 1e-9);
        assertEquals(1.4, SkillEffects.reelSpeedScale(9), 1e-9);
    }

    @Test
    void lineControlImprovesWithFishingLevel() {
        assertEquals(1.0, SkillEffects.lineControlScale(1), 1e-9);
        assertEquals(0.715, SkillEffects.lineControlScale(20), 1e-9);
        assertEquals(1.0, SkillEffects.lineControlScale(0), 1e-9);
    }

    @Test
    void applyToProfileScalesEveryField() {
        FishProfile p = SkillEffects.applyToProfile(new FishProfile(140f, 50, 1.0f, 1.0f), 1, 5, 5, 5);
        assertEquals(100f, p.stamina(), 1e-3);
        assertEquals(70, p.dashInterval());
        assertEquals(0.7f, p.dashPower(), 1e-3);
        assertEquals(1.0f, p.tensionGain(), 1e-3);
    }
}
