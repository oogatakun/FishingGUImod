package com.example.fishinggui.client;

/** Pure-Java mapping from fight values to gauge fractions and label states. */
public final class GaugeState {
    public enum Progress { START, BATTLE, ALMOST }

    public enum Line { SLACK, SAFE, DANGER }

    static final float START_BELOW = 0.2f;
    static final float ALMOST_FROM = 0.8f;
    static final float SLACK_BELOW = 25f;

    private GaugeState() {
    }

    public static float progress(float stamina, float maxStamina) {
        if (maxStamina <= 0f) {
            return 0f;
        }
        return Math.max(0f, Math.min(1f, 1f - stamina / maxStamina));
    }

    public static Progress progressLabel(float progress) {
        if (progress < START_BELOW) {
            return Progress.START;
        }
        return progress >= ALMOST_FROM ? Progress.ALMOST : Progress.BATTLE;
    }

    public static Line lineLabel(float tension, float danger) {
        if (tension < SLACK_BELOW) {
            return Line.SLACK;
        }
        return tension >= danger ? Line.DANGER : Line.SAFE;
    }
}
