package com.example.fishinggui.client;

import com.example.fishinggui.FishingGuiMod;
import com.example.fishinggui.entity.WormEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/** The earthworm: 18 body segments in a chain, from the model kit (geometry) with the crawl driven here. */
public class WormModel<T extends Entity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(FishingGuiMod.MOD_ID, "worm"), "main");
    private static final int SEGMENTS = 18;

    private final ModelPart root;
    private final ModelPart[] segs = new ModelPart[SEGMENTS];
    private final float[] baseYaw = new float[SEGMENTS];

    public WormModel(ModelPart root) {
        this.root = root;
        ModelPart parent = root.getChild("root");
        for (int i = 0; i < SEGMENTS; i++) {
            parent = parent.getChild("seg" + i);
            segs[i] = parent;
            baseYaw[i] = parent.yRot;
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();
        PartDefinition root_ = parts.addOrReplaceChild("root",
            CubeListBuilder.create(),
            PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition seg0 = root_.addOrReplaceChild("seg0",
            CubeListBuilder.create()
                .texOffs(11, 31).addBox(-0.574F, 0.2F, -0.225F, 1.148F, 1.0F, 1.7F)
                .texOffs(20, 31).addBox(-0.7F, -0.145F, -0.225F, 1.4F, 1.0F, 1.7F, new net.minecraft.client.model.geom.builders.CubeDeformation(0.0065F))
                .texOffs(27, 43).addBox(-0.5F, -0.39F, -0.225F, 1.0F, 1.0F, 1.7F, new net.minecraft.client.model.geom.builders.CubeDeformation(0.013F))
                .texOffs(34, 43).addBox(-0.6F, -0.3F, -1.0F, 1.2F, 1.4F, 1.0F),
            PartPose.offset(0.0F, -1.2F, -11.25F));
        PartDefinition seg1 = seg0.addOrReplaceChild("seg1",
            CubeListBuilder.create()
                .texOffs(29, 31).addBox(-0.902F, 0.2F, -0.225F, 1.804F, 1.0F, 1.7F, new net.minecraft.client.model.geom.builders.CubeDeformation(0.013F))
                .texOffs(11, 12).addBox(-1.1F, -0.67F, -0.225F, 2.2F, 1.37F, 1.7F)
                .texOffs(38, 31).addBox(-0.682F, -1.07F, -0.225F, 1.364F, 1.0F, 1.7F, new net.minecraft.client.model.geom.builders.CubeDeformation(0.0065F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 1.25F, (float) Math.toRadians(0.0), (float) Math.toRadians(-8.7831), (float) Math.toRadians(0.0)));
        PartDefinition seg2 = seg1.addOrReplaceChild("seg2",
            CubeListBuilder.create()
                .texOffs(9, 18).addBox(-1.107F, 0.2F, -0.225F, 2.214F, 1.0F, 1.7F, new net.minecraft.client.model.geom.builders.CubeDeformation(0.0325F))
                .texOffs(22, 12).addBox(-1.35F, -1.09F, -0.225F, 2.7F, 1.79F, 1.7F, new net.minecraft.client.model.geom.builders.CubeDeformation(0.013F))
                .texOffs(47, 31).addBox(-0.837F, -1.49F, -0.225F, 1.674F, 1.0F, 1.7F),
            PartPose.offsetAndRotation(0.0F, 0.0F, 1.25F, (float) Math.toRadians(0.0), (float) Math.toRadians(-7.2189), (float) Math.toRadians(0.0)));
        PartDefinition seg3 = seg2.addOrReplaceChild("seg3",
            CubeListBuilder.create()
                .texOffs(20, 18).addBox(-1.23F, 0.2F, -0.225F, 2.46F, 1.0F, 1.7F)
                .texOffs(39, 0).addBox(-1.5F, -1.35F, -0.225F, 3.0F, 2.05F, 1.7F, new net.minecraft.client.model.geom.builders.CubeDeformation(0.026F))
                .texOffs(0, 35).addBox(-0.93F, -1.75F, -0.225F, 1.86F, 1.0F, 1.7F),
            PartPose.offsetAndRotation(0.0F, 0.0F, 1.25F, (float) Math.toRadians(0.0), (float) Math.toRadians(-4.3998), (float) Math.toRadians(0.0)));
        PartDefinition seg4 = seg3.addOrReplaceChild("seg4",
            CubeListBuilder.create()
                .texOffs(31, 18).addBox(-1.23F, 0.2F, -0.225F, 2.46F, 1.0F, 1.7F, new net.minecraft.client.model.geom.builders.CubeDeformation(0.0195F))
                .texOffs(0, 6).addBox(-1.5F, -1.35F, -0.225F, 3.0F, 2.05F, 1.7F)
                .texOffs(9, 35).addBox(-0.93F, -1.75F, -0.225F, 1.86F, 1.0F, 1.7F, new net.minecraft.client.model.geom.builders.CubeDeformation(0.0195F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 1.25F, (float) Math.toRadians(0.0), (float) Math.toRadians(-0.816), (float) Math.toRadians(0.0)));
        PartDefinition seg5 = seg4.addOrReplaceChild("seg5",
            CubeListBuilder.create()
                .texOffs(42, 18).addBox(-1.435F, 0.2F, -0.225F, 2.87F, 1.0F, 1.7F)
                .texOffs(0, 0).addBox(-1.75F, -1.78F, -0.225F, 3.5F, 2.48F, 1.7F, new net.minecraft.client.model.geom.builders.CubeDeformation(0.013F))
                .texOffs(0, 23).addBox(-1.085F, -2.18F, -0.225F, 2.17F, 1.0F, 1.7F),
            PartPose.offsetAndRotation(0.0F, 0.0F, 1.25F, (float) Math.toRadians(0.0), (float) Math.toRadians(2.9096), (float) Math.toRadians(0.0)));
        PartDefinition seg6 = seg5.addOrReplaceChild("seg6",
            CubeListBuilder.create()
                .texOffs(11, 23).addBox(-1.435F, 0.2F, -0.225F, 2.87F, 1.0F, 1.7F, new net.minecraft.client.model.geom.builders.CubeDeformation(0.0195F))
                .texOffs(13, 0).addBox(-1.75F, -1.78F, -0.225F, 3.5F, 2.48F, 1.7F, new net.minecraft.client.model.geom.builders.CubeDeformation(0.0325F))
                .texOffs(22, 23).addBox(-1.085F, -2.18F, -0.225F, 2.17F, 1.0F, 1.7F, new net.minecraft.client.model.geom.builders.CubeDeformation(0.0195F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 1.25F, (float) Math.toRadians(0.0), (float) Math.toRadians(6.1295), (float) Math.toRadians(0.0)));
        PartDefinition seg7 = seg6.addOrReplaceChild("seg7",
            CubeListBuilder.create()
                .texOffs(33, 23).addBox(-1.435F, 0.2F, -0.225F, 2.87F, 1.0F, 1.7F)
                .texOffs(26, 0).addBox(-1.75F, -1.78F, -0.225F, 3.5F, 2.48F, 1.7F, new net.minecraft.client.model.geom.builders.CubeDeformation(0.0065F))
                .texOffs(44, 23).addBox(-1.085F, -2.18F, -0.225F, 2.17F, 1.0F, 1.7F),
            PartPose.offsetAndRotation(0.0F, 0.0F, 1.25F, (float) Math.toRadians(0.0), (float) Math.toRadians(8.284), (float) Math.toRadians(0.0)));
        PartDefinition seg8 = seg7.addOrReplaceChild("seg8",
            CubeListBuilder.create()
                .texOffs(0, 27).addBox(-1.23F, 0.2F, -0.225F, 2.46F, 1.0F, 1.7F, new net.minecraft.client.model.geom.builders.CubeDeformation(0.0195F))
                .texOffs(11, 6).addBox(-1.5F, -1.35F, -0.225F, 3.0F, 2.05F, 1.7F, new net.minecraft.client.model.geom.builders.CubeDeformation(0.026F))
                .texOffs(18, 35).addBox(-0.93F, -1.75F, -0.225F, 1.86F, 1.0F, 1.7F),
            PartPose.offsetAndRotation(0.0F, 0.0F, 1.25F, (float) Math.toRadians(0.0), (float) Math.toRadians(8.9985), (float) Math.toRadians(0.0)));
        PartDefinition seg9 = seg8.addOrReplaceChild("seg9",
            CubeListBuilder.create()
                .texOffs(11, 27).addBox(-1.23F, 0.2F, -0.225F, 2.46F, 1.0F, 1.7F)
                .texOffs(22, 6).addBox(-1.5F, -1.35F, -0.225F, 3.0F, 2.05F, 1.7F, new net.minecraft.client.model.geom.builders.CubeDeformation(0.0065F))
                .texOffs(27, 35).addBox(-0.93F, -1.75F, -0.225F, 1.86F, 1.0F, 1.7F, new net.minecraft.client.model.geom.builders.CubeDeformation(0.0195F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 1.25F, (float) Math.toRadians(0.0), (float) Math.toRadians(8.1489), (float) Math.toRadians(0.0)));
        PartDefinition seg10 = seg9.addOrReplaceChild("seg10",
            CubeListBuilder.create()
                .texOffs(22, 27).addBox(-1.23F, 0.2F, -0.225F, 2.46F, 1.0F, 1.7F, new net.minecraft.client.model.geom.builders.CubeDeformation(0.0195F))
                .texOffs(33, 6).addBox(-1.5F, -1.35F, -0.225F, 3.0F, 2.05F, 1.7F, new net.minecraft.client.model.geom.builders.CubeDeformation(0.026F))
                .texOffs(36, 35).addBox(-0.93F, -1.75F, -0.225F, 1.86F, 1.0F, 1.7F),
            PartPose.offsetAndRotation(0.0F, 0.0F, 1.25F, (float) Math.toRadians(0.0), (float) Math.toRadians(5.8828), (float) Math.toRadians(0.0)));
        PartDefinition seg11 = seg10.addOrReplaceChild("seg11",
            CubeListBuilder.create()
                .texOffs(33, 27).addBox(-1.23F, 0.2F, -0.225F, 2.46F, 1.0F, 1.7F)
                .texOffs(44, 6).addBox(-1.5F, -1.35F, -0.225F, 3.0F, 2.05F, 1.7F, new net.minecraft.client.model.geom.builders.CubeDeformation(0.0065F))
                .texOffs(45, 35).addBox(-0.93F, -1.75F, -0.225F, 1.86F, 1.0F, 1.7F, new net.minecraft.client.model.geom.builders.CubeDeformation(0.0195F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 1.25F, (float) Math.toRadians(0.0), (float) Math.toRadians(2.5942), (float) Math.toRadians(0.0)));
        PartDefinition seg12 = seg11.addOrReplaceChild("seg12",
            CubeListBuilder.create()
                .texOffs(44, 27).addBox(-1.23F, 0.2F, -0.225F, 2.46F, 1.0F, 1.7F, new net.minecraft.client.model.geom.builders.CubeDeformation(0.0195F))
                .texOffs(0, 12).addBox(-1.5F, -1.35F, -0.225F, 3.0F, 2.05F, 1.7F, new net.minecraft.client.model.geom.builders.CubeDeformation(0.026F))
                .texOffs(0, 39).addBox(-0.93F, -1.75F, -0.225F, 1.86F, 1.0F, 1.7F),
            PartPose.offsetAndRotation(0.0F, 0.0F, 1.25F, (float) Math.toRadians(0.0), (float) Math.toRadians(-1.1454), (float) Math.toRadians(0.0)));
        PartDefinition seg13 = seg12.addOrReplaceChild("seg13",
            CubeListBuilder.create()
                .texOffs(0, 31).addBox(-1.025F, 0.2F, -0.225F, 2.05F, 1.0F, 1.7F)
                .texOffs(33, 12).addBox(-1.25F, -0.92F, -0.225F, 2.5F, 1.62F, 1.7F, new net.minecraft.client.model.geom.builders.CubeDeformation(0.0065F))
                .texOffs(9, 39).addBox(-0.775F, -1.32F, -0.225F, 1.55F, 1.0F, 1.7F),
            PartPose.offsetAndRotation(0.0F, 0.0F, 1.25F, (float) Math.toRadians(0.0), (float) Math.toRadians(-4.6858), (float) Math.toRadians(0.0)));
        PartDefinition seg14 = seg13.addOrReplaceChild("seg14",
            CubeListBuilder.create()
                .texOffs(18, 39).addBox(-0.902F, 0.2F, -0.225F, 1.804F, 1.0F, 1.7F, new net.minecraft.client.model.geom.builders.CubeDeformation(0.0195F))
                .texOffs(44, 12).addBox(-1.1F, -0.67F, -0.225F, 2.2F, 1.37F, 1.7F, new net.minecraft.client.model.geom.builders.CubeDeformation(0.026F))
                .texOffs(27, 39).addBox(-0.682F, -1.07F, -0.225F, 1.364F, 1.0F, 1.7F),
            PartPose.offsetAndRotation(0.0F, 0.0F, 1.25F, (float) Math.toRadians(0.0), (float) Math.toRadians(-7.4118), (float) Math.toRadians(0.0)));
        PartDefinition seg15 = seg14.addOrReplaceChild("seg15",
            CubeListBuilder.create()
                .texOffs(36, 39).addBox(-0.779F, 0.2F, -0.225F, 1.558F, 1.0F, 1.7F)
                .texOffs(0, 18).addBox(-0.95F, -0.41F, -0.225F, 1.9F, 1.11F, 1.7F, new net.minecraft.client.model.geom.builders.CubeDeformation(0.0065F))
                .texOffs(45, 39).addBox(-0.589F, -0.81F, -0.225F, 1.178F, 1.0F, 1.7F, new net.minecraft.client.model.geom.builders.CubeDeformation(0.0195F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 1.25F, (float) Math.toRadians(0.0), (float) Math.toRadians(-8.8494), (float) Math.toRadians(0.0)));
        PartDefinition seg16 = seg15.addOrReplaceChild("seg16",
            CubeListBuilder.create()
                .texOffs(0, 43).addBox(-0.615F, 0.2F, -0.225F, 1.23F, 1.0F, 1.7F, new net.minecraft.client.model.geom.builders.CubeDeformation(0.0195F))
                .texOffs(9, 43).addBox(-0.75F, -0.185F, -0.225F, 1.5F, 1.0F, 1.7F)
                .texOffs(41, 43).addBox(-0.5F, -0.47F, -0.225F, 1.0F, 1.0F, 1.7F, new net.minecraft.client.model.geom.builders.CubeDeformation(0.0065F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 1.25F, (float) Math.toRadians(0.0), (float) Math.toRadians(-8.7488), (float) Math.toRadians(0.0)));
        PartDefinition seg17 = seg16.addOrReplaceChild("seg17",
            CubeListBuilder.create()
                .texOffs(48, 43).addBox(-0.5F, 0.2F, -0.225F, 1.0F, 1.0F, 1.7F, new net.minecraft.client.model.geom.builders.CubeDeformation(0.039F))
                .texOffs(18, 43).addBox(-0.6F, -0.06F, -0.225F, 1.2F, 1.0F, 1.7F)
                .texOffs(0, 47).addBox(-0.5F, -0.22F, -0.225F, 1.0F, 1.0F, 1.7F, new net.minecraft.client.model.geom.builders.CubeDeformation(0.013F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 1.25F, (float) Math.toRadians(0.0), (float) Math.toRadians(-7.1275), (float) Math.toRadians(0.0)));
        return LayerDefinition.create(mesh, 56, 56);
    }

    private static final float CURL_BEND = (float) Math.toRadians(20.0);

    /** The kit's curl: coils in over 0.3 s, holds until 0.8 s, relaxes by 1.4 s. */
    private static float curlAmount(float t) {
        if (t <= 0.0F || t >= 1.4F) {
            return 0.0F;
        }
        if (t < 0.3F) {
            return t / 0.3F;
        }
        return t < 0.8F ? 1.0F : (1.4F - t) / 0.6F;
    }

    /** The breeding dance lasts two seconds: in over 0.3, held, out over the last 0.5. */
    private static float breedAmount(float t) {
        if (t <= 0.0F || t >= 2.0F) {
            return 0.0F;
        }
        if (t < 0.3F) {
            return t / 0.3F;
        }
        return t < 1.5F ? 1.0F : (2.0F - t) / 0.5F;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // a wave runs down the body while it crawls; at rest the head probes slowly about
        float moving = Math.min(1.0F, limbSwingAmount * 6.0F);
        float curl = entity instanceof WormEntity worm ? curlAmount(worm.curlSeconds(ageInTicks)) : 0.0F;
        float breed = entity instanceof WormEntity worm ? breedAmount(worm.breedSeconds(ageInTicks)) : 0.0F;
        float breedTime = entity instanceof WormEntity worm ? worm.breedSeconds(ageInTicks) : 0.0F;
        for (int i = 0; i < SEGMENTS; i++) {
            float crawl = Mth.sin(limbSwing * 3.0F - i * 0.55F) * 0.38F * moving;
            float idle = Mth.sin(ageInTicks * 0.07F - i * 0.4F) * (0.04F + i * 0.004F) * (1.0F - moving);
            // the curl: every segment past the head bends 20 degrees, so the body coils up tight
            // breeding: the front of the body rears up and the whole body writhes
            float writhe = Mth.sin(breedTime * 12.0F - i * 0.7F) * 0.45F * breed;
            segs[i].xRot = i >= 1 && i <= 5 ? -0.22F * breed : 0.0F;
            segs[i].yRot = baseYaw[i] + crawl + idle + writhe - (i == 0 ? 0.0F : curl * CURL_BEND);
        }
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay,
                               float red, float green, float blue, float alpha) {
        root.render(pose, vc, light, overlay, red, green, blue, alpha);
    }
}
