package com.example.fishinggui.client;

import static com.example.fishinggui.food.TakoyakiRules.*;

import com.example.fishinggui.block.TakoyakiPanBlock;
import com.example.fishinggui.block.TakoyakiPanBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Draws the batter, octopus and takoyaki balls in the pan's twelve wells, coloured by how far along they are. */
public class TakoyakiPanRenderer implements BlockEntityRenderer<TakoyakiPanBlockEntity> {
    private static final ResourceLocation WHITE = new ResourceLocation("minecraft", "block/white_concrete");
    /** Well centres in block units (the model's 4 x 3 grid), facing south. */
    private static final double[] XS = {2, 6, 10, 14};
    private static final double[] ZS = {4, 8, 12};
    private static final double FLOOR = 3.4;

    public TakoyakiPanRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(TakoyakiPanBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light, int overlay) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(WHITE);
        VertexConsumer vc = buffers.getBuffer(RenderType.solid());
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-be.getBlockState().getValue(TakoyakiPanBlock.FACING).toYRot()));
        pose.translate(-0.5, 0, -0.5);
        pose.scale(1 / 16f, 1 / 16f, 1 / 16f);
        for (int xi = 0; xi < XS.length; xi++) {
            for (int zi = 0; zi < ZS.length; zi++) {
                int well = xi * ZS.length + zi;
                drawWell(vc, pose, sprite, light, XS[xi], ZS[zi], be.stage(well), be.toppings(well));
            }
        }
        pose.popPose();
    }

    private static void drawWell(VertexConsumer vc, PoseStack pose, TextureAtlasSprite sprite, int light, double cx,
            double cz, int stage, int toppings) {
        if (stage == EMPTY) {
            return;
        }
        if (stage == BATTER || stage == FILLED) {
            box(vc, pose, sprite, light, cx, cz, 2.2, FLOOR, FLOOR + 1.1, 245, 232, 176);
            if (stage == FILLED) {
                box(vc, pose, sprite, light, cx, cz, 1.2, FLOOR + 1.1, FLOOR + 1.5, 226, 150, 160);
            }
            return;
        }
        int r;
        int g;
        int b;
        switch (stage) {
            case COOKING -> { r = 240; g = 218; b = 148; }
            case FLIP -> { r = 224; g = 178; b = 84; }
            case COOKING_BACK -> { r = 214; g = 156; b = 66; }
            default -> { r = 170; g = 96; b = 36; }
        }
        // a ball built from stacked boxes: narrow, wide, wide, narrow
        box(vc, pose, sprite, light, cx, cz, 2.3, FLOOR, FLOOR + 0.9, r, g, b);
        box(vc, pose, sprite, light, cx, cz, 2.7, FLOOR + 0.9, FLOOR + 1.8, r, g, b);
        box(vc, pose, sprite, light, cx, cz, 2.5, FLOOR + 1.8, FLOOR + 2.5, r, g, b);
        box(vc, pose, sprite, light, cx, cz, 1.6, FLOOR + 2.5, FLOOR + 2.9, r, g, b);
        if (stage != READY) {
            return;
        }
        double top = FLOOR + 2.9;
        if ((toppings & MAYO) != 0) {
            box(vc, pose, sprite, light, cx, cz - 0.4, 1.6, 0.25, top, top + 0.2, 252, 248, 228);
            box(vc, pose, sprite, light, cx, cz + 0.4, 1.6, 0.25, top, top + 0.2, 252, 248, 228);
        }
        if ((toppings & AONORI) != 0) {
            box(vc, pose, sprite, light, cx - 0.5, cz + 0.1, 0.35, 0.35, top, top + 0.25, 64, 140, 60);
            box(vc, pose, sprite, light, cx + 0.5, cz - 0.3, 0.35, 0.35, top, top + 0.25, 64, 140, 60);
            box(vc, pose, sprite, light, cx + 0.1, cz + 0.6, 0.35, 0.35, top, top + 0.25, 64, 140, 60);
        }
    }

    private static void box(VertexConsumer vc, PoseStack pose, TextureAtlasSprite sprite, int light, double cx,
            double cz, double size, double y0, double y1, int r, int g, int b) {
        box(vc, pose, sprite, light, cx, cz, size, size, y0, y1, r, g, b);
    }

    private static void box(VertexConsumer vc, PoseStack pose, TextureAtlasSprite sprite, int light, double cx,
            double cz, double sx, double sz, double y0, double y1, int r, int g, int b) {
        float x0 = (float) (cx - sx / 2);
        float x1 = (float) (cx + sx / 2);
        float z0 = (float) (cz - sz / 2);
        float z1 = (float) (cz + sz / 2);
        float ya = (float) y0;
        float yb = (float) y1;
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        float u = (sprite.getU0() + sprite.getU1()) / 2;
        float v = (sprite.getV0() + sprite.getV1()) / 2;
        // top, then the four sides (the bottom is never seen)
        quad(vc, m, n, u, v, light, r, g, b, 0, 1, 0, x0, yb, z0, x0, yb, z1, x1, yb, z1, x1, yb, z0);
        quad(vc, m, n, u, v, light, r * 4 / 5, g * 4 / 5, b * 4 / 5, 0, 0, -1, x1, ya, z0, x1, yb, z0, x0, yb, z0, x0, ya, z0);
        quad(vc, m, n, u, v, light, r * 4 / 5, g * 4 / 5, b * 4 / 5, 0, 0, 1, x0, ya, z1, x0, yb, z1, x1, yb, z1, x1, ya, z1);
        quad(vc, m, n, u, v, light, r * 3 / 4, g * 3 / 4, b * 3 / 4, -1, 0, 0, x0, ya, z0, x0, yb, z0, x0, yb, z1, x0, ya, z1);
        quad(vc, m, n, u, v, light, r * 3 / 4, g * 3 / 4, b * 3 / 4, 1, 0, 0, x1, ya, z1, x1, yb, z1, x1, yb, z0, x1, ya, z0);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Matrix3f n, float u, float v, int light, int r, int g,
            int b, float nx, float ny, float nz, float... p) {
        for (int i = 0; i < 4; i++) {
            vc.vertex(m, p[i * 3], p[i * 3 + 1], p[i * 3 + 2]).color(r, g, b, 255).uv(u, v)
                    .uv2(light).normal(n, nx, ny, nz).endVertex();
        }
    }
}
