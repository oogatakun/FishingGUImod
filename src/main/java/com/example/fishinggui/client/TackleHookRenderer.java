package com.example.fishinggui.client;

import com.example.fishinggui.FishingGuiMod;
import com.example.fishinggui.item.TackleRodItem;
import com.example.fishinggui.tackle.Part;
import com.example.fishinggui.tackle.RodSetup;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * The bobber of a tackle rod: vanilla draws the line, and in place of the vanilla bobber the float, lure and hook
 * fitted to the rod float on the water. A hook cast with any other rod is drawn as before.
 */
public final class TackleHookRenderer extends FishingHookRenderer {
    private static final RenderType VANILLA_BOBBER =
            RenderType.entityCutout(new ResourceLocation("textures/entity/fishing_hook.png"));

    /** How wide the line is in the world, in blocks. */
    private static final double WORLD_WIDTH = 0.01;

    private final Map<String, ResourceLocation> textures = new HashMap<>();

    public TackleHookRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    private static ItemStack rodOf(FishingHook hook) {
        Player player = hook.getPlayerOwner();
        if (player == null) {
            return ItemStack.EMPTY;
        }
        if (player.getMainHandItem().getItem() instanceof TackleRodItem) {
            return player.getMainHandItem();
        }
        return player.getOffhandItem().getItem() instanceof TackleRodItem ? player.getOffhandItem() : ItemStack.EMPTY;
    }

    @Override
    public void render(FishingHook hook, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer,
            int light) {
        ItemStack rod = rodOf(hook);
        // the line's path is taken from vanilla and redrawn as a thin ribbon; the vanilla bobber is not drawn
        LineCollector line = new LineCollector();
        super.render(hook, yaw, partialTick, pose, type -> type == RenderType.lineStrip() ? line
                : type == VANILLA_BOBBER && !rod.isEmpty() ? Hidden.INSTANCE : buffer.getBuffer(type), light);
        drawLine(line.points, buffer, light);
        if (rod.isEmpty()) {
            return;
        }

        RodSetup setup = TackleRodItem.setupOf(rod);
        TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        pose.pushPose();
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        PoseStack.Pose last = pose.last();
        // from the bottom up: the hook under the water, the lure at the surface, the float sticking out above it
        Item bait = TackleRodItem.baitOf(rod);
        if (bait != null) {
            // the bait hangs on the hook, a little below it and drawn behind it
            TextureAtlasSprite sprite = Minecraft.getInstance().getItemRenderer().getItemModelShaper()
                    .getItemModel(bait).getParticleIcon();
            quad(consumer, last, sprite, 0.05f, -0.36f, 0.26f, light);
        }
        draw(consumer, last, atlas, setup, Part.HOOK, 0.0f, -0.26f, 0.22f, light);
        draw(consumer, last, atlas, setup, Part.LURE, 0.0f, -0.05f, 0.34f, light);
        draw(consumer, last, atlas, setup, Part.FLOAT, 0.0f, 0.15f, 0.30f, light);
        pose.popPose();
    }

    /**
     * The line as a flat ribbon with a width in the world, not in screen pixels like the vanilla line: it narrows with
     * distance, but never below one pixel and never above two and a half.
     */
    private static void drawLine(List<Vec3> points, MultiBufferSource buffer, int light) {
        if (points.size() < 2) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        double pixel = 2.0 * Math.tan(Math.toRadians(mc.options.fov().get()) / 2.0) / Math.max(1, mc.getWindow().getHeight());
        VertexConsumer out = buffer.getBuffer(RenderType.leash());
        for (int i = 0; i + 1 < points.size(); i++) {
            Vec3 a = points.get(i);
            Vec3 b = points.get(i + 1);
            Vec3 mid = a.add(b).scale(0.5);
            double distance = Math.max(0.05, mid.length());
            double width = Math.max(distance * pixel, Math.min(distance * pixel * 2.5, WORLD_WIDTH));
            Vec3 side = b.subtract(a).cross(mid);
            if (side.lengthSqr() < 1.0e-12) {
                continue;
            }
            Vec3 half = side.normalize().scale(width / 2.0);
            ribbonVertex(out, a.add(half), light);
            ribbonVertex(out, b.add(half), light);
            ribbonVertex(out, b.subtract(half), light);
            ribbonVertex(out, a.subtract(half), light);
        }
    }

    private static void ribbonVertex(VertexConsumer out, Vec3 p, int light) {
        out.vertex(p.x, p.y, p.z).color(0, 0, 0, 255).uv2(light).endVertex();
    }

    private void draw(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlas atlas, RodSetup setup, Part part,
            float cx, float cy, float size, int light) {
        int rank = setup.rank(part);
        if (rank == 0) {
            return;
        }
        TextureAtlasSprite sprite = atlas.getSprite(texture(part, rank, setup));
        quad(consumer, pose, sprite, cx, cy, size, light);
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite, float cx,
            float cy, float size, int light) {
        float h = size / 2.0f;
        vertex(consumer, pose, cx - h, cy - h, sprite.getU0(), sprite.getV1(), light);
        vertex(consumer, pose, cx + h, cy - h, sprite.getU1(), sprite.getV1(), light);
        vertex(consumer, pose, cx + h, cy + h, sprite.getU1(), sprite.getV0(), light);
        vertex(consumer, pose, cx - h, cy + h, sprite.getU0(), sprite.getV0(), light);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float u, float v,
            int light) {
        consumer.vertex(pose.pose(), x, y, 0.0f).color(255, 255, 255, 255).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0.0f, 1.0f, 0.0f)
                .endVertex();
    }

    /** The picture of a fitted part: its own art if it has any, else the plain part's. */
    private ResourceLocation texture(Part part, int rank, RodSetup setup) {
        String name = part.itemName(rank, setup.style(part));
        return textures.computeIfAbsent(name, n -> {
            boolean own = Minecraft.getInstance().getResourceManager().getResource(
                    new ResourceLocation(FishingGuiMod.MOD_ID, "textures/item/" + n + ".png")).isPresent();
            return new ResourceLocation(FishingGuiMod.MOD_ID, "item/" + (own ? n : part.itemName(rank)));
        });
    }

    /** Takes the points of vanilla's line (already in camera space) instead of drawing them. */
    private static final class LineCollector implements VertexConsumer {
        final List<Vec3> points = new ArrayList<>();
        private double x;
        private double y;
        private double z;

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return this;
        }

        @Override
        public void endVertex() {
            points.add(new Vec3(x, y, z));
        }

        @Override
        public void defaultColor(int r, int g, int b, int a) {
        }

        @Override
        public void unsetDefaultColor() {
        }
    }

    /** A vertex consumer that takes everything and draws nothing. */
    private static final class Hidden implements VertexConsumer {
        static final Hidden INSTANCE = new Hidden();

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return this;
        }

        @Override
        public void endVertex() {
        }

        @Override
        public void defaultColor(int r, int g, int b, int a) {
        }

        @Override
        public void unsetDefaultColor() {
        }
    }
}
