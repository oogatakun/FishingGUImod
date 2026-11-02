package com.example.fishinggui.fight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class FightSessionTest {
    private static final FishProfile CALM = new FishProfile(100f, 1_000_000, 1f, 1f);

    private static FightSession session(FishProfile profile) {
        return new FightSession(profile, new Random(42));
    }

    private static void tick(FightSession s, int n) {
        for (int i = 0; i < n; i++) {
            s.tick();
        }
    }

    /** Reels below tension 40 and releases above it: never slack, never near breaking. */
    private static void holdTension(FightSession s) {
        s.setReeling(s.tension() < 40f);
        s.tick();
    }

    @Test
    void holdingReelWithoutRestBreaksTheLine() {
        FightSession s = session(CALM);
        int ticks = 0;
        while (s.phase() == FightSession.Phase.ACTIVE && ticks < 200) {
            s.setReeling(true);
            s.tick();
            ticks++;
        }
        assertEquals(FightSession.Phase.FAILED, s.phase());
        assertEquals(67, ticks);
        assertEquals(FightSession.MAX_TENSION, s.tension());
    }

    @Test
    void releasingLowersTensionAndRecoversStamina() {
        FightSession s = session(CALM);
        s.setReeling(true);
        tick(s, 10);
        assertEquals(15f, s.tension(), 1e-3f);
        assertEquals(90f, s.stamina(), 1e-3f);
        s.setReeling(false);
        tick(s, 10);
        assertEquals(0f, s.tension(), 1e-3f);
        assertEquals(93f, s.stamina(), 1e-3f);
        assertEquals(FightSession.Phase.ACTIVE, s.phase());
    }

    @Test
    void pulsingTheReelWinsTheFight() {
        FightSession s = session(CALM);
        boolean reeling = true;
        int guard = 0;
        while (s.phase() == FightSession.Phase.ACTIVE && guard++ < 5000) {
            if (reeling && s.tension() >= 60f) {
                reeling = false;
            } else if (!reeling && s.tension() <= 10f) {
                reeling = true;
            }
            s.setReeling(reeling);
            s.tick();
        }
        assertEquals(FightSession.Phase.SUCCESS, s.phase());
    }

    @Test
    void successWhenStaminaReachesZero() {
        FightSession s = session(new FishProfile(5f, 1_000_000, 1f, 1f));
        s.setReeling(true);
        tick(s, 5);
        assertEquals(FightSession.Phase.SUCCESS, s.phase());
        assertEquals(0f, s.stamina());
    }

    @Test
    void dashRaisesTensionEvenWhenReleased() {
        // dashPower 2: -3 (release) + 5 (dash) = +2 per tick
        FightSession s = session(new FishProfile(100f, 1, 2f, 1f));
        s.setReeling(false);
        tick(s, 20);
        assertTrue(s.dashing());
        assertEquals(40f, s.tension(), 0.01f);
        assertEquals(100f, s.stamina(), 1e-3f);
    }

    @Test
    void reelingDuringDashReducesStamina() {
        FightSession s = session(new FishProfile(100f, 1, 1f, 1f));
        s.setReeling(true);
        tick(s, 5);
        assertTrue(s.dashing());
        assertEquals(95f, s.stamina(), 1e-3f);
        assertEquals(20f, s.tension(), 1e-3f);
    }

    @Test
    void dashPowerScalesDashTension() {
        FightSession s = session(new FishProfile(100f, 1, 2f, 1f));
        s.setReeling(false);
        s.tick();
        assertEquals(2f, s.tension(), 1e-3f);
    }

    @Test
    void tensionGainScalesReeling() {
        FightSession s = session(new FishProfile(100f, 1_000_000, 1f, 2f));
        s.setReeling(true);
        s.tick();
        assertEquals(3f, s.tension(), 1e-3f);
    }

    @Test
    void telegraphComesTenTicksBeforeDash() {
        FightSession s = session(new FishProfile(100f, 100, 1f, 1f));
        int guard = 0;
        while (!s.telegraphing() && guard++ < 300) {
            holdTension(s);
        }
        assertTrue(s.telegraphing());
        assertFalse(s.dashing());
        int extra = 0;
        while (!s.dashing() && extra < 50) {
            holdTension(s);
            extra++;
        }
        assertEquals(FightSession.TELEGRAPH_TICKS, extra);
    }

    @Test
    void silenceReleasesTheReel() {
        FightSession s = session(CALM);
        s.setReeling(true);
        tick(s, 40);
        assertTrue(s.reeling());
        s.tick();
        assertFalse(s.reeling());
    }

    @Test
    void repeatedInputKeepsTheReelAlive() {
        FightSession s = session(CALM);
        for (int i = 0; i < 3; i++) {
            s.setReeling(true);
            tick(s, 20);
        }
        assertTrue(s.reeling());
    }

    @Test
    void inputFloodIsLimitedPerTick() {
        FightSession s = session(CALM);
        s.setReeling(true);
        s.setReeling(false);
        s.setReeling(true);
        assertFalse(s.reeling());
        s.tick();
        s.setReeling(true);
        assertTrue(s.reeling());
    }

    @Test
    void endedSessionIgnoresFurtherTicks() {
        FightSession s = session(CALM);
        for (int i = 0; i < 100; i++) {
            s.setReeling(true);
            s.tick();
        }
        assertEquals(FightSession.Phase.FAILED, s.phase());
        float tension = s.tension();
        float stamina = s.stamina();
        s.tick();
        assertEquals(tension, s.tension());
        assertEquals(stamina, s.stamina());
    }

    private static final FishProfile NO_DASH = new FishProfile(100f, 1_000_000, 1f, 1f);

    @Test
    void idleSlackLineMakesTheFishEscape() {
        FightSession s = session(NO_DASH);
        tick(s, FightSession.SLACK_ESCAPE_TICKS - 1);
        assertEquals(FightSession.Phase.ACTIVE, s.phase());
        s.tick();
        assertEquals(FightSession.Phase.FAILED, s.phase());
        assertFalse(s.lineBroke());
    }

    @Test
    void keepingTensionUpResetsTheSlackTimer() {
        FightSession s = session(new FishProfile(100_000f, 1_000_000, 1f, 1f));
        for (int i = 0; i < 400; i++) {
            holdTension(s);
            assertEquals(FightSession.Phase.ACTIVE, s.phase());
        }
        // The counter was reset: another slack_escape-1 idle ticks (with tension decaying) must not end it.
        s.setReeling(false);
        tick(s, FightSession.SLACK_ESCAPE_TICKS - 20);
        assertEquals(FightSession.Phase.ACTIVE, s.phase());
    }

    @Test
    void realSnapReportsLineBroke() {
        FightSession s = session(CALM);
        while (s.phase() == FightSession.Phase.ACTIVE) {
            s.setReeling(true);
            s.tick();
        }
        assertEquals(FightSession.Phase.FAILED, s.phase());
        assertTrue(s.lineBroke());
    }

    @Test
    void slackTimerDoesNotOverrideSuccessOnTheSameTick() {
        FightSession s = session(new FishProfile(1f, 1_000_000, 1f, 1f));
        for (int i = 0; i < FightSession.SLACK_ESCAPE_TICKS - 1; i++) {
            s.setReeling(false);
            s.tick();
        }
        assertEquals(FightSession.Phase.ACTIVE, s.phase());
        s.setReeling(true);
        s.tick(); // 80th slack tick, but stamina reaches 0 on this very tick
        assertEquals(FightSession.Phase.SUCCESS, s.phase());
        assertFalse(s.lineBroke());
    }
}
