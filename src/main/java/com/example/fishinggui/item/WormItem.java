package com.example.fishinggui.item;

import com.example.fishinggui.entity.ModEntities;
import com.example.fishinggui.entity.WormEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A worm: bait, and right-clicked onto the top of a dirt block it is put down as a living worm. */
public class WormItem extends Item {
    public WormItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (context.getClickedFace() != Direction.UP || !level.getBlockState(pos).is(BlockTags.DIRT)) {
            if (!level.isClientSide && player != null) {
                player.displayClientMessage(Component.translatable("message.fishinggui.worm.dirt_only"), true);
            }
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            WormEntity worm = ModEntities.WORM.get().create(level);
            if (worm == null) {
                return InteractionResult.FAIL;
            }
            Vec3 at = context.getClickLocation();
            worm.moveTo(at.x, pos.getY() + 1.0, at.z, level.random.nextFloat() * 360.0F, 0.0F);
            if (!level.noCollision(worm)) {
                return InteractionResult.FAIL;
            }
            level.addFreshEntity(worm);
            level.playSound(null, pos, SoundEvents.SLIME_SQUISH_SMALL, SoundSource.NEUTRAL, 0.6F, 1.0F);
            if (player == null || !player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
