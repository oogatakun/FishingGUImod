package com.example.fishinggui.client;

import com.example.fishinggui.fight.FightSession;

/** Client entry points called by the S2C packets. Bodies are implemented in Task 5. */
public final class ClientFightHooks {
    private ClientFightHooks() {
    }

    public static void onStart(float maxStamina) {
    }

    public static void onState(float tension, float stamina, boolean telegraph, boolean dashing,
                               FightSession.Phase phase) {
    }
}
