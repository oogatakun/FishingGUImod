package com.example.fishinggui.block;

import static com.example.fishinggui.food.TakoyakiRules.*;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The twelve wells of a takoyaki pan: a stage, a cooking timer and toppings per well. */
public class TakoyakiPanBlockEntity extends BlockEntity {
    private final int[] stage = new int[WELLS];
    private final int[] timer = new int[WELLS];
    private final int[] toppings = new int[WELLS];

    public TakoyakiPanBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.TAKOYAKI_PAN_BE.get(), pos, state);
    }

    public int stage(int well) {
        return stage[well];
    }

    public int toppings(int well) {
        return toppings[well];
    }

    /** How far a cooking well is towards its next stage, 0..1. */
    public float progress(int well) {
        return Math.min(1f, timer[well] / (float) COOK_TICKS);
    }

    public boolean anyCooking() {
        for (int s : stage) {
            if (isCooking(s)) {
                return true;
            }
        }
        return false;
    }

    /** Second pour on filled wells; otherwise the first pour on empty ones. Returns the wells changed. */
    public int pour() {
        int to = count(FILLED) > 0 ? COOKING : BATTER;
        int from = to == COOKING ? FILLED : EMPTY;
        return advance(from, to);
    }

    /** Puts up to {@code available} pieces of octopus into wells that have batter. */
    public int addOctopus(int available) {
        int n = 0;
        for (int i = 0; i < WELLS && n < available; i++) {
            if (stage[i] == BATTER) {
                set(i, FILLED);
                n++;
            }
        }
        return changed(n);
    }

    public int flip() {
        return advance(FLIP, COOKING_BACK);
    }

    /** Adds a topping to every ready well that does not have it yet. */
    public int topping(int bit) {
        int n = 0;
        for (int i = 0; i < WELLS; i++) {
            if (stage[i] == READY && (toppings[i] & bit) == 0) {
                toppings[i] |= bit;
                n++;
            }
        }
        return changed(n);
    }

    public int count(int wantedStage) {
        int n = 0;
        for (int s : stage) {
            if (s == wantedStage) {
                n++;
            }
        }
        return n;
    }

    public boolean readyWithoutTopping() {
        for (int i = 0; i < WELLS; i++) {
            if (stage[i] == READY && toppings[i] == 0) {
                return true;
            }
        }
        return false;
    }

    /** Takes up to {@code max} ready, topped takoyaki out of the pan. */
    public int collect(int max) {
        int n = 0;
        for (int i = 0; i < WELLS && n < max; i++) {
            if (stage[i] == READY && toppings[i] != 0) {
                set(i, EMPTY);
                n++;
            }
        }
        return changed(n);
    }

    private int advance(int from, int to) {
        int n = 0;
        for (int i = 0; i < WELLS; i++) {
            if (stage[i] == from) {
                set(i, to);
                n++;
            }
        }
        return changed(n);
    }

    private void set(int well, int newStage) {
        stage[well] = newStage;
        timer[well] = 0;
        if (newStage == EMPTY) {
            toppings[well] = 0;
        }
    }

    private int changed(int n) {
        if (n > 0) {
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
        return n;
    }

    /** Keeps the colour progress moving between server updates; the server's timers overwrite it on each sync. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, TakoyakiPanBlockEntity be) {
        if (!state.getValue(TakoyakiPanBlock.POWERED)) {
            return;
        }
        for (int i = 0; i < WELLS; i++) {
            if (isCooking(be.stage[i]) && be.timer[i] < COOK_TICKS) {
                be.timer[i]++;
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TakoyakiPanBlockEntity be) {
        if (!state.getValue(TakoyakiPanBlock.POWERED)) {
            return;
        }
        boolean sync = false;
        for (int i = 0; i < WELLS; i++) {
            if (isCooking(be.stage[i]) && ++be.timer[i] >= COOK_TICKS) {
                be.set(i, afterCooking(be.stage[i]));
                sync = true;
            }
        }
        if (sync) {
            be.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putIntArray("Stage", stage);
        tag.putIntArray("Timer", timer);
        tag.putIntArray("Toppings", toppings);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        read(tag.getIntArray("Stage"), stage);
        read(tag.getIntArray("Timer"), timer);
        read(tag.getIntArray("Toppings"), toppings);
    }

    private static void read(int[] from, int[] into) {
        System.arraycopy(from, 0, into, 0, Math.min(from.length, into.length));
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
