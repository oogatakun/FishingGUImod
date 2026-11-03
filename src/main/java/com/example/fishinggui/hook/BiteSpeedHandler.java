package com.example.fishinggui.hook;

import com.example.fishinggui.FishingGuiMod;
import com.example.fishinggui.fight.FightManager;
import com.example.fishinggui.skill.SkillData;
import com.example.fishinggui.skill.SkillEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Quick Bite skill: while the bobber waits, shave an extra tick off the wait so the wait shrinks by 15% per level. */
@Mod.EventBusSubscriber(modid = FishingGuiMod.MOD_ID)
public final class BiteSpeedHandler {
    private BiteSpeedHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        FishingHook hook = player.fishing;
        if (hook == null || FightManager.isFighting(player) || hook.nibble != 0) {
            return;
        }
        double extra = SkillEffects.biteSpeedExtraTicks(SkillData.get(player).levelOf(SkillEffects.BITE_SPEED));
        if (extra <= 0) {
            return;
        }
        int steps = (int) extra + (player.getRandom().nextDouble() < extra - (int) extra ? 1 : 0);
        for (int i = 0; i < steps; i++) {
            if (hook.timeUntilLured > 1) {
                hook.timeUntilLured--;
            } else if (hook.timeUntilLured <= 0 && hook.timeUntilHooked > 1) {
                hook.timeUntilHooked--;
            } else {
                break;
            }
        }
    }
}
