package com.example.fishinggui.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WormBreedingTest {
    @Test
    void youngNeedRoomInTheArea() {
        assertTrue(WormBreeding.roomFor(2));
        assertTrue(WormBreeding.roomFor(6));
        assertFalse(WormBreeding.roomFor(7));
        assertFalse(WormBreeding.roomFor(8));
    }
}
