package com.example.fishinggui.client;

import com.example.fishinggui.block.ShichirinBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Draws the fish lying on the grill: raw while cooking, cooked or burnt item once done. */
public class ShichirinRenderer implements BlockEntityRenderer<ShichirinBlockEntity> {
    private static final double[][] SPOTS = {{0.34, 0.5}, {0.66, 0.5}};

    private final BlockEntityRendererProvider.Context ctx;

    public ShichirinRenderer(BlockEntityRendererProvider.Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void render(ShichirinBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light, int overlay) {
        for (int i = 0; i < ShichirinBlockEntity.SLOTS; i++) {
            ItemStack stack = be.display(i);
            if (stack.isEmpty()) {
                continue;
            }
            pose.pushPose();
            pose.translate(SPOTS[i][0], 0.68, SPOTS[i][1]);
            pose.mulPose(Axis.XP.rotationDegrees(90));
            pose.mulPose(Axis.ZP.rotationDegrees(i == 0 ? 30 : -30));
            pose.scale(0.42f, 0.42f, 0.42f);
            ctx.getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                    pose, buffers, be.getLevel(), (int) be.getBlockPos().asLong() + i);
            pose.popPose();
        }
    }
}
