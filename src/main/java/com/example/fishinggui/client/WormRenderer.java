package com.example.fishinggui.client;

import com.example.fishinggui.FishingGuiMod;
import com.example.fishinggui.entity.WormEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Draws the earthworm, authored about 22 units long, down to a worm's size. */
public class WormRenderer extends MobRenderer<WormEntity, WormModel<WormEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(FishingGuiMod.MOD_ID, "textures/entity/earthworm.png");
    private static final float SCALE = 0.23F;

    public WormRenderer(EntityRendererProvider.Context context) {
        super(context, new WormModel<>(context.bakeLayer(WormModel.LAYER)), 0.08F);
    }

    @Override
    protected void scale(WormEntity entity, PoseStack pose, float partialTick) {
        pose.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(WormEntity entity) {
        return TEXTURE;
    }
}
