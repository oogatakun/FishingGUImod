package com.example.fishinggui.food;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.example.fishinggui.food.GrillSlot.Phase;
import org.junit.jupiter.api.Test;

class GrillSlotTest {
    private static GrillSlot run(GrillSlot s, int ticks, boolean lit) {
        for (int i = 0; i < ticks; i++) {
            s = s.tick(lit);
        }
        return s;
    }

    @Test
    void cooksAtCookTicksThenBurns() {
        GrillSlot s = GrillSlot.fresh(1);
        assertEquals(Phase.RAW, run(s, 199, true).phase());
        assertEquals(Phase.COOKED, run(s, 200, true).phase());
        assertEquals(Phase.COOKED, run(s, 599, true).phase());
        assertEquals(Phase.BURNT, run(s, 600, true).phase());
    }

    @Test
    void levelScalesCookTime() {
        assertEquals(Phase.RAW, run(GrillSlot.fresh(20), 579, true).phase());
        assertEquals(Phase.COOKED, run(GrillSlot.fresh(20), 580, true).phase());
    }

    @Test
    void unlitPausesProgress() {
        GrillSlot s = run(GrillSlot.fresh(1), 100, true);
        assertEquals(100, run(s, 500, false).progress());
        assertEquals(Phase.RAW, run(s, 500, false).phase());
    }

    @Test
    void burntStaysBurnt() {
        GrillSlot b = run(GrillSlot.fresh(1), 10000, true);
        assertEquals(Phase.BURNT, b.phase());
        assertEquals(600, b.progress());
        assertSame(b, b.tick());
    }
}
