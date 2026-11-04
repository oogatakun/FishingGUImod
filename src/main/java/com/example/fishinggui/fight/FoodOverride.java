package com.example.fishinggui.fight;

/** Per-fish override of the cooked fish's food values (nutrition, saturation modifier, heal in half-hearts). */
public record FoodOverride(int nutrition, float saturation, int heal) {
}
