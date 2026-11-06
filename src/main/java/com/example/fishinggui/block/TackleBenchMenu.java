package com.example.fishinggui.block;

import com.example.fishinggui.item.ModItems;
import com.example.fishinggui.item.TacklePartItem;
import com.example.fishinggui.item.TackleRodItem;
import com.example.fishinggui.tackle.Part;
import com.example.fishinggui.tackle.RodSetup;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The tackle bench: one slot per part, a slot to take a finished rod from, and a slot that takes a rod apart again.
 * The server decides what each slot accepts and what the rod comes out as.
 */
public class TackleBenchMenu extends AbstractContainerMenu {
    public static final List<Part> PARTS = availableParts();
    public static final int PER_ROW = 4;
    public static final int SLOT = 18;
    public static final int PITCH = 24;
    public static final int WIDTH = 224;
    public static final int HEIGHT = 190;
    public static final int INV_Y = 108;

    private final ContainerLevelAccess access;
    private final Container parts = new SimpleContainer(PARTS.size() + 1) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };
    private final Container result = new SimpleContainer(1);
    private final int disassembleSlot = PARTS.size();
    private final int resultSlot = PARTS.size() + 1;

    private static List<Part> availableParts() {
        List<Part> list = new ArrayList<>();
        for (Part p : Part.values()) {
            if (p.available()) {
                list.add(p);
            }
        }
        return List.copyOf(list);
    }

    public static int partX(int index) {
        return 14 + (index % PER_ROW) * PITCH;
    }

    public static int partY(int index) {
        return 26 + (index / PER_ROW) * PITCH;
    }

    /** First y below the rows of part slots. */
    public static int toolsY() {
        return partY(PARTS.size() - 1) + PITCH + 8;
    }

    public static int disassembleX() {
        return 14;
    }

    public static int resultX() {
        return 14 + (PER_ROW - 1) * PITCH;
    }

    public TackleBenchMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.TACKLE_BENCH.get(), id);
        this.access = access;
        for (int i = 0; i < PARTS.size(); i++) {
            Part part = PARTS.get(i);
            addSlot(new Slot(parts, i, partX(i), partY(i)) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof TacklePartItem p && p.part() == part;
                }

                @Override
                public int getMaxStackSize() {
                    return part == Part.ROD_BLANK ? 1 : 16;
                }
            });
        }
        addSlot(new Slot(parts, disassembleSlot, disassembleX(), toolsY()) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof TackleRodItem && TackleRodItem.setupOf(stack).complete();
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(result, 0, resultX(), toolsY()) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                for (int i = 0; i < PARTS.size(); i++) {
                    parts.removeItem(i, 1);
                }
                access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS,
                        0.5f, 1.4f));
                slotsChanged(parts);
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, (WIDTH - 162) / 2 + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, (WIDTH - 162) / 2 + col * 18, INV_Y + 58));
        }
    }

    /** The parts currently in the part slots. */
    public RodSetup setup() {
        RodSetup setup = new RodSetup();
        for (int i = 0; i < PARTS.size(); i++) {
            if (parts.getItem(i).getItem() instanceof TacklePartItem p) {
                setup = setup.with(p.part(), p.rank());
            }
        }
        return setup;
    }

    @Override
    public void slotsChanged(Container changed) {
        if (changed != parts) {
            return;
        }
        ItemStack rod = parts.getItem(disassembleSlot);
        if (!rod.isEmpty() && isPartsEmpty()) {
            takeApart(rod);
        }
        RodSetup setup = setup();
        ItemStack out = ItemStack.EMPTY;
        if (setup.complete()) {
            out = TackleRodItem.assemble(setup, parts.getItem(0).getDamageValue());
        }
        result.setItem(0, out);
        broadcastChanges();
    }

    private boolean isPartsEmpty() {
        for (int i = 0; i < PARTS.size(); i++) {
            if (!parts.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Puts the parts of a finished rod back into their slots; the rod blank keeps the rod's wear. */
    private void takeApart(ItemStack rod) {
        RodSetup setup = TackleRodItem.setupOf(rod);
        for (int i = 0; i < PARTS.size(); i++) {
            Part part = PARTS.get(i);
            int rank = setup.rank(part);
            if (rank > 0) {
                ItemStack stack = new ItemStack(ModItems.PARTS.get(part).get(rank - 1).get());
                if (part == Part.ROD_BLANK) {
                    stack.setDamageValue(rod.getDamageValue());
                }
                parts.setItem(i, stack);
            }
        }
        parts.setItem(disassembleSlot, ItemStack.EMPTY);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int benchEnd = resultSlot + 1;
        if (index == resultSlot) {
            if (!moveItemStackTo(stack, benchEnd, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, copy);
            return copy;
        }
        if (index < benchEnd) {
            if (!moveItemStackTo(stack, benchEnd, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof TackleRodItem) {
            if (!moveItemStackTo(stack, disassembleSlot, disassembleSlot + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof TacklePartItem p) {
            int at = PARTS.indexOf(p.part());
            if (at < 0 || !moveItemStackTo(stack, at, at + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.TACKLE_BENCH.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, parts));
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != result && super.canTakeItemForPickAll(stack, slot);
    }
}
