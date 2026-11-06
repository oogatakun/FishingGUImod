package com.example.fishinggui.block;

import com.example.fishinggui.item.LureItem;
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
    public static final int WIDTH = 256;
    public static final int HEIGHT = 236;
    public static final int INV_Y = 152;

    private final ContainerLevelAccess access;
    private final boolean serverSide;
    /** The parts laid out from the rod in the disassemble slot, as they were; null when nothing is laid out. */
    private List<ItemStack> preview;
    private boolean busy;
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
        this.serverSide = !inventory.player.level().isClientSide;
        for (int i = 0; i < PARTS.size(); i++) {
            Part part = PARTS.get(i);
            addSlot(new Slot(parts, i, partX(i), partY(i)) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return parts.getItem(disassembleSlot).isEmpty()
                            && stack.getItem() instanceof TacklePartItem p && p.part() == part;
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
                return stack.getItem() instanceof TackleRodItem && TackleRodItem.setupOf(stack).complete()
                        && isPartsEmpty();
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
                if (p instanceof LureItem) {
                    setup = setup.withLureSpecies(LureItem.speciesOf(parts.getItem(i)));
                }
            }
        }
        return setup;
    }

    @Override
    public void slotsChanged(Container changed) {
        if (changed != parts || busy) {
            return;
        }
        ItemStack rod = parts.getItem(disassembleSlot);
        if (!serverSide) {
            // the server alone decides what the disassembly does; the client only mirrors its slots
        } else if (preview != null) {
            if (rod.isEmpty()) {
                // the rod was taken back out untouched: the laid-out parts vanish again
                clearPartSlots();
            } else if (!previewIntact()) {
                // a part was taken: the rod is now taken apart for good
                busy = true;
                parts.setItem(disassembleSlot, ItemStack.EMPTY);
                busy = false;
                preview = null;
            }
        } else if (!rod.isEmpty() && isPartsEmpty()) {
            layOut(rod);
        }
        RodSetup setup = setup();
        ItemStack out = ItemStack.EMPTY;
        if (setup.complete() && parts.getItem(disassembleSlot).isEmpty()) {
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

    private void clearPartSlots() {
        busy = true;
        for (int i = 0; i < PARTS.size(); i++) {
            parts.setItem(i, ItemStack.EMPTY);
        }
        busy = false;
        preview = null;
    }

    private boolean previewIntact() {
        for (int i = 0; i < PARTS.size(); i++) {
            if (!ItemStack.matches(parts.getItem(i), preview.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Lays the parts of a finished rod out in their slots; the rod blank keeps the rod's wear. The rod stays in its
     * slot until one of the parts is taken, and taking it back out removes the laid-out parts again.
     */
    private void layOut(ItemStack rod) {
        RodSetup setup = TackleRodItem.setupOf(rod);
        busy = true;
        for (int i = 0; i < PARTS.size(); i++) {
            Part part = PARTS.get(i);
            int rank = setup.rank(part);
            if (rank > 0) {
                ItemStack stack = new ItemStack(ModItems.PARTS.get(part).get(rank - 1).get());
                if (part == Part.ROD_BLANK) {
                    stack.setDamageValue(rod.getDamageValue());
                }
                if (part == Part.LURE && setup.lureSpecies() != null) {
                    LureItem.setSpecies(stack, setup.lureSpecies());
                }
                parts.setItem(i, stack);
            }
        }
        busy = false;
        preview = new ArrayList<>();
        for (int i = 0; i < PARTS.size(); i++) {
            preview.add(parts.getItem(i).copy());
        }
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
        if (preview != null) {
            // the rod is still whole in its slot: hand back the rod, not the parts laid out beside it
            clearPartSlots();
        }
        access.execute((level, pos) -> clearContainer(player, parts));
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != result && super.canTakeItemForPickAll(stack, slot);
    }
}
