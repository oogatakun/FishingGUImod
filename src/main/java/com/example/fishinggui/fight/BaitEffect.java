package com.example.fishinggui.fight;

/** Maps a bait to its effect on a fight. Pure Java so the numbers stay unit-testable. */
public final class BaitEffect {
    static final float GLOW_BUG_LUCK = 2.0f;
    static final float STURDY_PASTE_DASH_FACTOR = 0.6f;

    private BaitEffect() {
    }

    public static FishProfile apply(BaitType bait, FishProfile base) {
        if (bait == BaitType.STURDY_PASTE) {
            return new FishProfile(base.stamina(), base.dashInterval(),
                    base.dashPower() * STURDY_PASTE_DASH_FACTOR, base.tensionGain());
        }
        return base;
    }

    public static float luckBonus(BaitType bait) {
        return bait == BaitType.GLOW_BUG ? GLOW_BUG_LUCK : 0f;
    }
}
