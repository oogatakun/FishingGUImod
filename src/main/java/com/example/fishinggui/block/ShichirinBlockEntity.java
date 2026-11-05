package com.example.fishinggui.block;

import com.example.fishinggui.food.CookingRules;
import com.example.fishinggui.food.GrillSlot;
import com.example.fishinggui.item.FishLevels;
import com.example.fishinggui.item.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

public class ShichirinBlockEntity extends BlockEntity {
    public static final int SLOTS = 4;
    public static final int MAX_FUEL_TICKS = CookingRules.MAX_FUEL * CookingRules.FUEL_TICKS;

    private int fuelTicks;
    private final ItemStack[] raw = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    private final int[] progress = new int[SLOTS];

    public ShichirinBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.SHICHIRIN_BE.get(), pos, state);
    }

    public int fuelTicks() {
        return fuelTicks;
    }

    public boolean tryAddFuel() {
        if (fuelTicks + CookingRules.FUEL_TICKS > MAX_FUEL_TICKS) {
            return false;
        }
        fuelTicks += CookingRules.FUEL_TICKS;
        changed();
        return true;
    }

    public boolean tryPlace(ItemStack held) {
        for (int i = 0; i < SLOTS; i++) {
            if (raw[i].isEmpty()) {
                raw[i] = held.copyWithCount(1);
                progress[i] = 0;
                changed();
                return true;
            }
        }
        return false;
    }

    private static String idOf(ItemStack s) {
        ResourceLocation k = ForgeRegistries.ITEMS.getKey(s.getItem());
        return k == null ? "" : k.toString();
    }

    private GrillSlot slotState(int i) {
        Integer lv = FishLevels.BY_ID.get(idOf(raw[i]));
        return new GrillSlot(lv == null ? 1 : lv, progress[i]);
    }

    public GrillSlot.Phase phase(int i) {
        return slotState(i).phase();
    }

    /** What the slot currently looks like / yields: raw fish, cooked fish or burnt fish. */
    public ItemStack display(int i) {
        if (raw[i].isEmpty()) {
            return ItemStack.EMPTY;
        }
        switch (phase(i)) {
            case BURNT:
                return new ItemStack(ModItems.BURNT_FISH.get());
            case COOKED:
                String id = CookingRules.cookedIdFor(idOf(raw[i]));
                Item it = id == null ? null : ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
                return it == null ? raw[i].copy() : new ItemStack(it);
            default:
                return raw[i].copy();
        }
    }

    /** Block-local (x, z) of each grill slot, shared with the renderer. */
    public static final double[][] SPOTS = {{0.33, 0.35}, {0.67, 0.35}, {0.33, 0.65}, {0.67, 0.65}};

    /**
     * Remove and return what the slot nearest to the block-local point (fx, fz) holds: the finished
     * item, or the raw fish if it is still cooking. EMPTY if that slot is empty or nothing is placed.
     */
    public ItemStack takeOutNear(double fx, double fz) {
        int best = -1;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < SLOTS; i++) {
            if (raw[i].isEmpty()) {
                continue;
            }
            double dx = SPOTS[i][0] - fx;
            double dz = SPOTS[i][1] - fz;
            double d = dx * dx + dz * dz;
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        if (best < 0) {
            return ItemStack.EMPTY;
        }
        return clear(best, phase(best) == GrillSlot.Phase.RAW ? raw[best].copy() : display(best));
    }

    private ItemStack clear(int i, ItemStack result) {
        raw[i] = ItemStack.EMPTY;
        progress[i] = 0;
        changed();
        return result;
    }

    /** What to drop when the block is removed. */
    public List<ItemStack> contents() {
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            if (!raw[i].isEmpty()) {
                out.add(display(i));
            }
        }
        return out;
    }

    private void changed() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ShichirinBlockEntity be) {
        boolean lit = be.fuelTicks > 0;
        boolean sync = false;
        if (lit) {
            be.fuelTicks--;
            for (int i = 0; i < SLOTS; i++) {
                if (be.raw[i].isEmpty()) {
                    continue;
                }
                GrillSlot before = be.slotState(i);
                GrillSlot after = before.tick();
                be.progress[i] = after.progress();
                if (after.phase() != before.phase()) {
                    sync = true;
                }
            }
            be.setChanged();
        }
        boolean nowLit = be.fuelTicks > 0;
        if (state.getValue(ShichirinBlock.LIT) != nowLit) {
            level.setBlock(pos, state.setValue(ShichirinBlock.LIT, nowLit), 3);
        }
        if (sync) {
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Fuel", fuelTicks);
        for (int i = 0; i < SLOTS; i++) {
            tag.put("Raw" + i, raw[i].save(new CompoundTag()));
            tag.putInt("Progress" + i, progress[i]);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        fuelTicks = Math.min(tag.getInt("Fuel"), MAX_FUEL_TICKS);
        for (int i = 0; i < SLOTS; i++) {
            raw[i] = tag.contains("Raw" + i) ? ItemStack.of(tag.getCompound("Raw" + i)) : ItemStack.EMPTY;
            progress[i] = tag.getInt("Progress" + i);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
