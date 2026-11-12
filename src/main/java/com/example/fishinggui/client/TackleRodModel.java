package com.example.fishinggui.client;

import com.example.fishinggui.FishingGuiMod;
import com.example.fishinggui.item.TackleRodItem;
import com.example.fishinggui.tackle.Part;
import com.example.fishinggui.tackle.RodSetup;
import com.example.fishinggui.tackle.Style;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.mojang.math.Transformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.SimpleModelState;
import net.minecraftforge.client.model.geometry.UnbakedGeometryHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The look of a tackle rod, put together from the parts fitted to it: the rod blank's picture, with the reel at the
 * handle, the line along the rod and the float, lure and hook hanging from it. The overlay pictures are made by
 * the art scripts (rodreel_*, rodline_*, rodfloat_*, rodlure_*, rodhook_*), one per part variant, and are layered here.
 */
public final class TackleRodModel extends BakedModelWrapper<BakedModel> {
    private static final ResourceLocation LOCATION = new ResourceLocation(FishingGuiMod.MOD_ID, "item/tackle_rod");

    /** Where the bait hangs on the rod picture (0..1 from the top left) and how big it is. */
    private static final float BAIT_X = 0.93f;
    private static final float BAIT_Y = 0.58f;
    private static final float BAIT_SIZE = 0.20f;

    private final Map<String, BakedModel> cache = new HashMap<>();
    private final ItemOverrides overrides = new ItemOverrides() {
        @Override
        @Nullable
        public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level,
                @Nullable LivingEntity entity, int seed) {
            RodSetup setup = TackleRodItem.setupOf(stack);
            if (setup.rank(Part.ROD_BLANK) == 0) {
                return originalModel.getOverrides().resolve(model, stack, level, entity, seed);
            }
            boolean cast = ClientEvents.isCast(stack, entity);
            Item bait = cast ? null : TackleRodItem.baitOf(stack);
            return cache.computeIfAbsent(key(setup, cast, bait), k -> compose(setup, cast, bait));
        }
    };

    public TackleRodModel(BakedModel original) {
        super(original);
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }

    private static String variant(RodSetup setup, Part part) {
        int rank = setup.rank(part);
        if (rank == 0) {
            return null;
        }
        Style style = setup.style(part);
        return (style == null ? "" : style.id() + "_") + Part.RANKS[rank - 1];
    }

    private static String key(RodSetup setup, boolean cast, Item bait) {
        StringBuilder sb = new StringBuilder(cast ? "cast" : "idle").append(',').append(bait);
        for (Part part : Part.values()) {
            sb.append(',').append(variant(setup, part));
        }
        return sb.toString();
    }

    /** One picture layered on the rod, drawn in the given transform (identity for the full-size overlays). */
    private record Layer(TextureAtlasSprite sprite, ModelState state) {
    }

    private BakedModel compose(RodSetup setup, boolean cast, Item bait) {
        String state = cast ? "cast" : "idle";
        List<ResourceLocation> before = new ArrayList<>();
        before.add(texture(Part.ROD_BLANK.itemName(setup.rank(Part.ROD_BLANK), setup.style(Part.ROD_BLANK))));
        addLayer(before, "rodreel_", variant(setup, Part.REEL), null);
        addLayer(before, "rodline_", variant(setup, Part.LINE), state);
        List<ResourceLocation> after = new ArrayList<>();
        if (!cast) {
            // once cast, the float, lure, hook and bait are out on the water (see TackleHookRenderer)
            addLayer(before, "rodfloat_", variant(setup, Part.FLOAT), state);
            addLayer(after, "rodhook_", variant(setup, Part.HOOK), state);
            addLayer(after, "rodlure_", variant(setup, Part.LURE), state);
        }

        TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
        ModelState identity = BlockModelRotation.X0_Y0;
        List<Layer> layers = new ArrayList<>();
        before.forEach(t -> layers.add(new Layer(atlas.getSprite(t), identity)));
        if (bait != null) {
            // the bait hangs on the hook: its picture, small, behind the hook
            TextureAtlasSprite sprite = Minecraft.getInstance().getItemRenderer().getItemModelShaper()
                    .getItemModel(bait).getParticleIcon();
            layers.add(new Layer(sprite, new SimpleModelState(new Transformation(
                    new Vector3f(BAIT_X - 0.5f, 0.5f - BAIT_Y, 0.0f), new Quaternionf(),
                    new Vector3f(BAIT_SIZE, BAIT_SIZE, 1.0f), new Quaternionf()))));
        }
        after.forEach(t -> layers.add(new Layer(atlas.getSprite(t), identity)));

        SimpleBakedModel.Builder builder = new SimpleBakedModel.Builder(originalModel.useAmbientOcclusion(),
                originalModel.usesBlockLight(), originalModel.isGui3d(), originalModel.getTransforms(),
                ItemOverrides.EMPTY);
        builder.particle(layers.get(0).sprite());
        for (int i = 0; i < layers.size(); i++) {
            TextureAtlasSprite sprite = layers.get(i).sprite();
            var elements = UnbakedGeometryHelper.createUnbakedItemElements(i, sprite.contents(), null);
            for (BakedQuad quad : UnbakedGeometryHelper.bakeElements(elements, material -> sprite,
                    layers.get(i).state(), LOCATION)) {
                builder.addUnculledFace(quad);
            }
        }
        return builder.build();
    }

    private static void addLayer(List<ResourceLocation> layers, String prefix, String variant, String state) {
        if (variant != null) {
            layers.add(texture(prefix + variant + (state == null ? "" : "_" + state)));
        }
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(FishingGuiMod.MOD_ID, "item/" + name);
    }
}
