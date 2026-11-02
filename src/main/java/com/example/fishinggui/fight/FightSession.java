package com.example.fishinggui.fight;

import java.util.Random;

/** Server-side rules of one tension fight. Pure Java: no Minecraft types. */
public final class FightSession {
    public enum Phase { ACTIVE, SUCCESS, FAILED }

    public static final float MAX_TENSION = 100f;
    public static final float DANGER_TENSION = 70f;
    public static final float SLACK_TENSION = 25f;

    static final float REEL_TENSION = 1.5f;
    static final float REEL_STAMINA = 1.0f;
    static final float RELEASE_TENSION = 3.0f;
    static final float RELEASE_STAMINA_RECOVER = 0.3f;
    static final float DASH_TENSION = 2.5f;
    static final int TELEGRAPH_TICKS = 10;
    static final int DASH_MIN_TICKS = 20;
    static final int DASH_MAX_TICKS = 40;
    static final int INPUT_TIMEOUT_TICKS = 40;
    static final int MAX_INPUTS_PER_TICK = 2;
    static final int SLACK_ESCAPE_TICKS = 80;

    private final FishProfile profile;
    private final Random random;

    private float tension;
    private float stamina;
    private boolean reeling;
    private int ticksUntilDash;
    private int dashTicksLeft;
    private int silentTicks;
    private int inputsThisTick;
    private int slackTicks;
    private boolean lineBroke;
    private Phase phase = Phase.ACTIVE;

    public FightSession(FishProfile profile, Random random) {
        this.profile = profile;
        this.random = random;
        this.stamina = profile.stamina();
        this.ticksUntilDash = nextDashInterval();
    }

    /** Applies a reel/release input. Inputs beyond {@link #MAX_INPUTS_PER_TICK} within one tick are ignored. */
    public void setReeling(boolean reeling) {
        if (inputsThisTick >= MAX_INPUTS_PER_TICK) {
            return;
        }
        inputsThisTick++;
        silentTicks = 0;
        this.reeling = reeling;
    }

    public void tick() {
        if (phase != Phase.ACTIVE) {
            return;
        }
        inputsThisTick = 0;
        if (++silentTicks > INPUT_TIMEOUT_TICKS) {
            reeling = false;
        }
        updateDash();
        boolean dashing = dashTicksLeft > 0;

        if (reeling) {
            tension += REEL_TENSION * profile.tensionGain();
            stamina -= REEL_STAMINA;
        } else {
            tension -= RELEASE_TENSION;
            stamina = Math.min(profile.stamina(), stamina + RELEASE_STAMINA_RECOVER);
        }
        if (dashing) {
            tension += DASH_TENSION * profile.dashPower();
        }
        tension = Math.max(0f, tension);

        if (tension >= MAX_TENSION) {
            tension = MAX_TENSION;
            phase = Phase.FAILED;
            lineBroke = true;
        } else if (stamina <= 0f) {
            stamina = 0f;
            phase = Phase.SUCCESS;
        } else if (tension < SLACK_TENSION) {
            if (++slackTicks >= SLACK_ESCAPE_TICKS) {
                phase = Phase.FAILED;
            }
        } else {
            slackTicks = 0;
        }
    }

    private void updateDash() {
        if (dashTicksLeft > 0) {
            dashTicksLeft--;
            if (dashTicksLeft == 0) {
                ticksUntilDash = nextDashInterval();
            }
            return;
        }
        if (--ticksUntilDash <= 0) {
            dashTicksLeft = DASH_MIN_TICKS + random.nextInt(DASH_MAX_TICKS - DASH_MIN_TICKS + 1);
        }
    }

    private int nextDashInterval() {
        return Math.max(1, Math.round(profile.dashInterval() * (0.6f + 0.8f * random.nextFloat())));
    }

    public float tension() { return tension; }
    public float stamina() { return stamina; }
    public float maxStamina() { return profile.stamina(); }
    public boolean reeling() { return reeling; }
    public boolean dashing() { return dashTicksLeft > 0; }
    public boolean telegraphing() { return dashTicksLeft == 0 && ticksUntilDash <= TELEGRAPH_TICKS; }
    public Phase phase() { return phase; }
    /** True only if the fight ended because the line snapped (not a slack escape). */
    public boolean lineBroke() { return lineBroke; }
}
