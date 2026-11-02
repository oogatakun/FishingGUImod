package com.example.fishinggui.client;

import com.example.fishinggui.fight.FightSession;
import net.minecraft.client.Minecraft;

/** Client entry points called by the S2C packets. */
public final class ClientFightHooks {
    private static FightScreen screen;

    private ClientFightHooks() {
    }

    public static void onStart(float maxStamina) {
        screen = new FightScreen(maxStamina);
        Minecraft.getInstance().setScreen(screen);
    }

    public static void onState(float tension, float stamina, boolean telegraph, boolean dashing,
                               FightSession.Phase phase) {
        if (screen != null) {
            screen.apply(tension, stamina, telegraph, dashing, phase);
        }
    }

    static void clear(FightScreen closed) {
        if (screen == closed) {
            screen = null;
        }
    }
}
