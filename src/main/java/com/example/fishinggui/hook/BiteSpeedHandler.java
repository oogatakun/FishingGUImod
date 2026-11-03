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

/** Quick Bite skill: while the bobber waits, shave an extra tick off the wait with probability 0.1 per level. */
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
        double bonus = SkillEffects.biteSpeedBonus(SkillData.get(player).levelOf(SkillEffects.BITE_SPEED));
        if (bonus <= 0 || player.getRandom().nextDouble() >= bonus) {
            return;
        }
        if (hook.timeUntilLured > 0) {
            hook.timeUntilLured--;
        } else if (hook.timeUntilHooked > 0) {
            hook.timeUntilHooked--;
        }
    }
}
