package com.example.fishinggui.fight;

public record FishProfile(float stamina, int dashInterval, float dashPower, float tensionGain) {
    public static final FishProfile DEFAULT = new FishProfile(100f, 70, 1.0f, 1.0f);
}
