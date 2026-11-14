package com.example.fishinggui.client;

import com.example.fishinggui.sieve.SieveItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** How the sieve moves while sifting: both arms out front, the sieve shaken side to side. */
public final class SieveClientExtensions implements IClientItemExtensions {
    public static final SieveClientExtensions INSTANCE = new SieveClientExtensions();

    /** Shakes per second, as an angular speed in radians per tick. */
    private static final float SHAKE_SPEED = 1.7F;

    private static final HumanoidModel.ArmPose SIFTING = HumanoidModel.ArmPose.create("ORESIEVE_SIFTING", true,
            (model, entity, arm) -> {
                float time = entity.tickCount + Minecraft.getInstance().getFrameTime();
                float shake = Mth.sin(time * SHAKE_SPEED) * 0.12F;
                model.rightArm.xRot = -0.95F;
                model.leftArm.xRot = -0.95F;
                model.rightArm.yRot = -0.15F + shake;
                model.leftArm.yRot = 0.15F + shake;
                model.rightArm.zRot = 0.0F;
                model.leftArm.zRot = 0.0F;
            });

    private SieveClientExtensions() {
    }

    /** One small grain of the sifted block, falling. */
    public static void spawnGrain(BlockState state, double x, double y, double z) {
        Particle grain = Minecraft.getInstance().particleEngine.createParticle(
                new BlockParticleOption(ParticleTypes.BLOCK, state), x, y, z, 0.0, -0.05, 0.0);
        if (grain != null) {
            grain.scale(0.35F);
        }
    }

    private static boolean isSifting(LivingEntity entity, ItemStack stack) {
        return entity.isUsingItem() && entity.getUseItem() == stack && stack.is(SieveItems.SIEVE.get());
    }

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        return isSifting(entity, stack) ? SIFTING : null;
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack stack,
            float partialTick, float equipProcess, float swingProcess) {
        if (!isSifting(player, stack)) {
            return false;
        }
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        float time = player.getTicksUsingItem() + partialTick;
        // Ease from the resting hand position to the middle of the screen over the first few ticks.
        float in = Mth.clamp(time / 5.0F, 0.0F, 1.0F);
        in = in * in * (3.0F - 2.0F * in);
        float x = Mth.lerp(in, side * 0.56F, side * 0.10F);
        float y = Mth.lerp(in, -0.52F, -0.40F);
        float z = Mth.lerp(in, -0.72F, -0.95F);
        poseStack.translate(x, y + equipProcess * -0.6F, z);

        // The shake itself: side to side, with a little hop and roll at each end.
        float wave = Mth.sin(time * SHAKE_SPEED) * in;
        float hop = Math.abs(Mth.cos(time * SHAKE_SPEED)) * in;
        poseStack.translate(wave * 0.09F, hop * 0.025F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-wave * 5.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(hop * 3.0F));
        return true;
    }
}
