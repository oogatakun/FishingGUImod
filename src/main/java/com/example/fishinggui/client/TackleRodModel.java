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
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.geometry.UnbakedGeometryHelper;
import org.jetbrains.annotations.Nullable;

/**
 * The look of a tackle rod, put together from the parts fitted to it: the rod blank's picture, with the reel at the
 * handle, the line along the rod and the float, lure and hook hanging from it. The overlay pictures are made by
 * the art scripts (rodreel_*, rodline_*, rodfloat_*, rodlure_*, rodhook_*), one per part variant, and are layered here.
 */
public final class TackleRodModel extends BakedModelWrapper<BakedModel> {
    private static final ResourceLocation LOCATION = new ResourceLocation(FishingGuiMod.MOD_ID, "item/tackle_rod");

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
            return cache.computeIfAbsent(key(setup, cast), k -> compose(setup, cast));
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

    private static String key(RodSetup setup, boolean cast) {
        StringBuilder sb = new StringBuilder(cast ? "cast" : "idle");
        for (Part part : Part.values()) {
            sb.append(',').append(variant(setup, part));
        }
        return sb.toString();
    }

    private BakedModel compose(RodSetup setup, boolean cast) {
        String state = cast ? "cast" : "idle";
        List<ResourceLocation> layers = new ArrayList<>();
        layers.add(texture(Part.ROD_BLANK.itemName(setup.rank(Part.ROD_BLANK), setup.style(Part.ROD_BLANK))));
        addLayer(layers, "rodreel_", variant(setup, Part.REEL), null);
        addLayer(layers, "rodline_", variant(setup, Part.LINE), state);
        addLayer(layers, "rodfloat_", variant(setup, Part.FLOAT), state);
        addLayer(layers, "rodhook_", variant(setup, Part.HOOK), state);
        addLayer(layers, "rodlure_", variant(setup, Part.LURE), state);

        TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
        ModelState modelState = BlockModelRotation.X0_Y0;
        SimpleBakedModel.Builder builder = new SimpleBakedModel.Builder(originalModel.useAmbientOcclusion(),
                originalModel.usesBlockLight(), originalModel.isGui3d(), originalModel.getTransforms(),
                ItemOverrides.EMPTY);
        builder.particle(atlas.getSprite(layers.get(0)));
        for (int i = 0; i < layers.size(); i++) {
            TextureAtlasSprite sprite = atlas.getSprite(layers.get(i));
            var elements = UnbakedGeometryHelper.createUnbakedItemElements(i, sprite.contents(), null);
            for (BakedQuad quad : UnbakedGeometryHelper.bakeElements(elements, material -> sprite, modelState,
                    LOCATION)) {
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
