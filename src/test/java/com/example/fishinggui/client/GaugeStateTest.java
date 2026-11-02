package com.example.fishinggui.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GaugeStateTest {
    @Test
    void progressIsInverseOfStaminaAndClamped() {
        assertEquals(0f, GaugeState.progress(100f, 100f), 1e-6f);
        assertEquals(0.5f, GaugeState.progress(50f, 100f), 1e-6f);
        assertEquals(1f, GaugeState.progress(0f, 100f), 1e-6f);
        assertEquals(1f, GaugeState.progress(-5f, 100f), 1e-6f);
        assertEquals(0f, GaugeState.progress(120f, 100f), 1e-6f);
        assertEquals(0f, GaugeState.progress(10f, 0f), 1e-6f);
    }

    @Test
    void progressLabelBoundaries() {
        assertEquals(GaugeState.Progress.START, GaugeState.progressLabel(0f));
        assertEquals(GaugeState.Progress.START, GaugeState.progressLabel(0.19f));
        assertEquals(GaugeState.Progress.BATTLE, GaugeState.progressLabel(0.2f));
        assertEquals(GaugeState.Progress.BATTLE, GaugeState.progressLabel(0.79f));
        assertEquals(GaugeState.Progress.ALMOST, GaugeState.progressLabel(0.8f));
        assertEquals(GaugeState.Progress.ALMOST, GaugeState.progressLabel(1f));
    }

    @Test
    void lineLabelBoundaries() {
        assertEquals(GaugeState.Line.SLACK, GaugeState.lineLabel(0f, 70f));
        assertEquals(GaugeState.Line.SLACK, GaugeState.lineLabel(24.9f, 70f));
        assertEquals(GaugeState.Line.SAFE, GaugeState.lineLabel(25f, 70f));
        assertEquals(GaugeState.Line.SAFE, GaugeState.lineLabel(69.9f, 70f));
        assertEquals(GaugeState.Line.DANGER, GaugeState.lineLabel(70f, 70f));
        assertEquals(GaugeState.Line.DANGER, GaugeState.lineLabel(100f, 70f));
    }
}
