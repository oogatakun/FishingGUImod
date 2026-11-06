package com.example.fishinggui.block;

import static com.example.fishinggui.food.TakoyakiRules.*;

import com.example.fishinggui.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Server side of the pan screen: one tool applied to one well, paid for from the player's inventory. */
public final class TakoyakiService {
    private static final double REACH_SQ = 8.0 * 8.0;

    private TakoyakiService() {
    }

    public static Item toolItem(int tool) {
        return switch (tool) {
            case TOOL_BATTER -> ModItems.TAKOYAKI_BATTER.get();
            case TOOL_OCTOPUS -> ModItems.CUT_OCTOPUS.get();
            case TOOL_PICK -> ModItems.ICE_PICK.get();
            case TOOL_MAYO -> ModItems.MAYONNAISE.get();
            case TOOL_AONORI -> ModItems.AONORI.get();
            default -> Items.BOWL;
        };
    }

    public static void apply(ServerPlayer player, BlockPos pos, int tool, int mask) {
        if (tool < 0 || tool >= TOOLS || mask == 0 || (mask & ~((1 << WELLS) - 1)) != 0
                || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > REACH_SQ
                || !(player.level().getBlockEntity(pos) instanceof TakoyakiPanBlockEntity be)) {
            return;
        }
        boolean creative = player.getAbilities().instabuild;
        boolean any = false;
        if (tool == TOOL_BOWL) {
            any = scoop(be, player, mask, creative);
        } else {
            for (int well = 0; well < WELLS; well++) {
                if ((mask >> well & 1) != 0) {
                    any |= useOn(player, be, tool, well, creative);
                }
            }
        }
        if (any) {
            player.level().playSound(null, pos, soundFor(tool), SoundSource.BLOCKS, 0.8f, 1.0f);
        } else {
            String key = tool == TOOL_BOWL && be.readyWithoutTopping()
                    ? "gui.fishinggui.takoyaki.no_topping" : "gui.fishinggui.takoyaki.nothing";
            player.displayClientMessage(Component.translatable(key), true);
        }
    }

    private static SoundEvent soundFor(int tool) {
        return switch (tool) {
            case TOOL_OCTOPUS -> SoundEvents.SLIME_BLOCK_PLACE;
            case TOOL_PICK -> SoundEvents.ITEM_FRAME_ROTATE_ITEM;
            case TOOL_MAYO, TOOL_AONORI -> SoundEvents.HONEY_BLOCK_SLIDE;
            case TOOL_BOWL -> SoundEvents.ITEM_PICKUP;
            default -> SoundEvents.BOTTLE_EMPTY;
        };
    }

    /** One tool on one well, paid for from the inventory. */
    private static boolean useOn(ServerPlayer player, TakoyakiPanBlockEntity be, int tool, int well,
            boolean creative) {
        Inventory inv = player.getInventory();
        int slot = findSlot(inv, toolItem(tool));
        if (slot < 0 && !creative) {
            return false;
        }
        ItemStack stack = slot < 0 ? ItemStack.EMPTY : inv.getItem(slot);
        boolean done;
        switch (tool) {
            case TOOL_BATTER -> {
                done = be.pourWell(well);
                if (done && !creative) {
                    stack.hurtAndBreak(1, player, p -> { });
                }
            }
            case TOOL_OCTOPUS -> {
                done = be.addOctopusWell(well);
                if (done && !creative) {
                    stack.shrink(1);
                }
            }
            case TOOL_PICK -> {
                done = be.flipWell(well);
                if (done && !creative) {
                    stack.hurtAndBreak(1, player, p -> { });
                }
            }
            case TOOL_MAYO -> {
                done = be.toppingWell(well, MAYO);
                if (done && !creative) {
                    useMayonnaise(inv, slot, stack);
                }
            }
            default -> {
                done = be.toppingWell(well, AONORI);
                if (done && !creative) {
                    stack.shrink(1);
                }
            }
        }
        return done;
    }

    /** Hotbar and main inventory first, then the off hand. */
    private static int findSlot(Inventory inv, Item item) {
        for (int i = 0; i < inv.items.size(); i++) {
            if (inv.items.get(i).is(item)) {
                return i;
            }
        }
        return inv.offhand.get(0).is(item) ? inv.items.size() + inv.armor.size() : -1;
    }

    /** One use of the bottle; the sixth leaves an empty glass bottle behind. */
    private static void useMayonnaise(Inventory inv, int slot, ItemStack stack) {
        if (stack.getDamageValue() + 1 >= stack.getMaxDamage()) {
            inv.setItem(slot, new ItemStack(Items.GLASS_BOTTLE));
        } else {
            stack.setDamageValue(stack.getDamageValue() + 1);
        }
    }

    /** Bowl: scoop the chosen wells (up to one boat) of finished takoyaki. */
    private static boolean scoop(TakoyakiPanBlockEntity be, ServerPlayer player, int mask, boolean creative) {
        Inventory inv = player.getInventory();
        int slot = findSlot(inv, Items.BOWL);
        if (slot < 0 && !creative) {
            return false;
        }
        int n = be.collectWells(mask, PORTIONS);
        if (n == 0) {
            return false;
        }
        if (!creative) {
            inv.getItem(slot).shrink(1);
        }
        ItemStack boat = new ItemStack(ModItems.TAKOYAKI.get());
        boat.setDamageValue(PORTIONS - n);
        if (!inv.add(boat)) {
            player.drop(boat, false);
        }
        return true;
    }
}
