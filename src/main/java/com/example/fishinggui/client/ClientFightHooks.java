package com.example.fishinggui.client;

import com.example.fishinggui.fight.FightSession;
import com.example.fishinggui.fight.Rarity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/** Client entry points called by the S2C packets. */
public final class ClientFightHooks {
    private static FightScreen screen;

    private ClientFightHooks() {
    }

    public static void onStart(float maxStamina) {
        if (screen != null) {
            screen.markFinished();
        }
        screen = new FightScreen(maxStamina);
        Minecraft.getInstance().setScreen(screen);
    }

    public static void onState(float tension, float stamina, boolean telegraph, boolean dashing,
                               FightSession.Phase phase) {
        if (screen != null) {
            screen.apply(tension, stamina, telegraph, dashing, phase);
        }
    }

    public static void onCatchResult(ItemStack stack, byte rarity, float weight, int xp) {
        Rarity[] all = Rarity.values();
        Rarity r = all[Math.max(0, Math.min(all.length - 1, rarity))];
        Minecraft.getInstance().setScreen(new CatchScreen(stack, r, weight, xp));
    }

    static void clear(FightScreen closed) {
        if (screen == closed) {
            screen = null;
        }
    }
}
