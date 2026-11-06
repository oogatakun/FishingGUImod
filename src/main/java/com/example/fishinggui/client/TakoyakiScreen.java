package com.example.fishinggui.client;

import static com.example.fishinggui.food.TakoyakiRules.*;

import com.example.fishinggui.block.TakoyakiPanBlock;
import com.example.fishinggui.block.TakoyakiPanBlockEntity;
import com.example.fishinggui.block.TakoyakiService;
import com.example.fishinggui.net.ModNetwork;
import com.example.fishinggui.net.TakoyakiActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The pan: pick a tool in the bar, then click wells one at a time. The server decides every action. */
public class TakoyakiScreen extends Screen {
    private static final int CELL = 34;
    private static final int GAP = 4;
    private static final int COLS = 4;
    private static final int ROWS = 3;
    private static final int TOOL = 26;
    private static final int GRID_W = COLS * CELL + (COLS - 1) * GAP;
    private static final int GRID_H = ROWS * CELL + (ROWS - 1) * GAP;
    private static final int PAD = 12;

    private final BlockPos pos;
    private int tool = -1;

    public TakoyakiScreen(BlockPos pos) {
        super(Component.translatable("block.fishinggui.takoyaki_pan"));
        this.pos = pos;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int panelW() {
        return Math.max(GRID_W, TOOLS * TOOL) + PAD * 2;
    }

    private int left() {
        return (width - panelW()) / 2;
    }

    private int top() {
        return (height - (GRID_H + TOOL + 70)) / 2;
    }

    private int gridLeft() {
        return left() + (panelW() - GRID_W) / 2;
    }

    private int gridTop() {
        return top() + 26;
    }

    private int toolLeft() {
        return left() + (panelW() - TOOLS * TOOL) / 2;
    }

    private int toolTop() {
        return gridTop() + GRID_H + 14;
    }

    private TakoyakiPanBlockEntity pan() {
        var level = Minecraft.getInstance().level;
        return level != null && level.getBlockEntity(pos) instanceof TakoyakiPanBlockEntity be ? be : null;
    }

    private static ItemStack findTool(int tool) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return ItemStack.EMPTY;
        }
        Item item = TakoyakiService.toolItem(tool);
        Inventory inv = player.getInventory();
        for (ItemStack s : inv.items) {
            if (s.is(item)) {
                return s;
            }
        }
        return inv.offhand.get(0).is(item) ? inv.offhand.get(0) : ItemStack.EMPTY;
    }

    private static boolean usable(int tool) {
        var player = Minecraft.getInstance().player;
        return player != null && (player.getAbilities().instabuild || !findTool(tool).isEmpty());
    }

    @Override
    public void tick() {
        var player = Minecraft.getInstance().player;
        if (pan() == null || player == null
                || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64) {
            onClose();
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int t = 0; t < TOOLS; t++) {
            if (inside(mx, my, toolLeft() + t * TOOL, toolTop(), TOOL - 2, TOOL - 2)) {
                tool = tool == t ? -1 : t;
                return true;
            }
        }
        for (int well = 0; well < WELLS; well++) {
            if (inside(mx, my, cellX(well), cellY(well), CELL, CELL)) {
                if (tool >= 0) {
                    ModNetwork.CHANNEL.sendToServer(new TakoyakiActionPacket(pos, tool, well));
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Wells are numbered column by column, like the block's renderer. */
    private int cellX(int well) {
        return gridLeft() + (well / ROWS) * (CELL + GAP);
    }

    private int cellY(int well) {
        return gridTop() + (well % ROWS) * (CELL + GAP);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.fill(left(), top(), left() + panelW(), top() + GRID_H + TOOL + 70, 0xE0202020);
        g.drawCenteredString(font, title, width / 2, top() + 8, 0xFFFFFF);
        TakoyakiPanBlockEntity be = pan();
        if (be == null) {
            return;
        }
        Component hovered = null;
        for (int well = 0; well < WELLS; well++) {
            drawWell(g, be, well);
            if (inside(mouseX, mouseY, cellX(well), cellY(well), CELL, CELL)) {
                hovered = Component.translatable("gui.fishinggui.takoyaki.stage." + be.stage(well));
            }
        }
        ItemStack hoveredTool = ItemStack.EMPTY;
        for (int t = 0; t < TOOLS; t++) {
            int x = toolLeft() + t * TOOL;
            int y = toolTop();
            boolean ok = usable(t);
            g.fill(x, y, x + TOOL - 2, y + TOOL - 2, t == tool ? 0xFFFFFFFF : 0xFF606060);
            g.fill(x + 1, y + 1, x + TOOL - 3, y + TOOL - 3, ok ? 0xFF2E2E2E : 0xFF151515);
            ItemStack shown = findTool(t);
            if (shown.isEmpty()) {
                shown = new ItemStack(TakoyakiService.toolItem(t));
            }
            g.renderItem(shown, x + 3, y + 3);
            if (ok) {
                g.renderItemDecorations(font, shown, x + 3, y + 3);
            } else {
                g.fill(x + 1, y + 1, x + TOOL - 3, y + TOOL - 3, 0x99000000);
            }
            if (inside(mouseX, mouseY, x, y, TOOL - 2, TOOL - 2)) {
                hoveredTool = shown;
            }
        }
        Component hint = be.getBlockState().getValue(TakoyakiPanBlock.POWERED)
                ? Component.translatable("gui.fishinggui.takoyaki.hint")
                : Component.translatable("gui.fishinggui.takoyaki.off");
        g.drawCenteredString(font, hint, width / 2, toolTop() + TOOL + 8, 0xAAAAAA);
        super.render(g, mouseX, mouseY, partialTick);
        if (!hoveredTool.isEmpty()) {
            g.renderTooltip(font, hoveredTool, mouseX, mouseY);
        } else if (hovered != null) {
            g.renderTooltip(font, hovered, mouseX, mouseY);
        }
    }

    private void drawWell(GuiGraphics g, TakoyakiPanBlockEntity be, int well) {
        int x = cellX(well);
        int y = cellY(well);
        int stage = be.stage(well);
        int border = stage == FLIP ? 0xFFFFD34D : 0xFF4A4A4A;
        g.fill(x, y, x + CELL, y + CELL, border);
        g.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, 0xFF1A1A1A);
        if (stage == EMPTY) {
            return;
        }
        int in = 5;
        if (stage == BATTER || stage == FILLED) {
            g.fill(x + in, y + in, x + CELL - in, y + CELL - in, 0xFFF5E8B0);
            if (stage == FILLED) {
                g.fill(x + CELL / 2 - 4, y + CELL / 2 - 4, x + CELL / 2 + 4, y + CELL / 2 + 4, 0xFFE29AA0);
            }
            return;
        }
        int[] rgb = color(stage, be.progress(well));
        g.fill(x + in, y + in, x + CELL - in, y + CELL - in, 0xFF000000 | rgb[0] << 16 | rgb[1] << 8 | rgb[2]);
        if (isCooking(stage)) {
            int w = Math.round((CELL - 4) * be.progress(well));
            g.fill(x + 2, y + CELL - 4, x + CELL - 2, y + CELL - 2, 0xFF000000);
            g.fill(x + 2, y + CELL - 4, x + 2 + w, y + CELL - 2, 0xFFFF9A3C);
        }
        if (stage == READY) {
            int t = be.toppings(well);
            if ((t & MAYO) != 0) {
                g.fill(x + 9, y + 12, x + CELL - 9, y + 14, 0xFFFCF8E4);
                g.fill(x + 9, y + 19, x + CELL - 9, y + 21, 0xFFFCF8E4);
            }
            if ((t & AONORI) != 0) {
                g.fill(x + 11, y + 9, x + 14, y + 12, 0xFF408C3C);
                g.fill(x + 20, y + 16, x + 23, y + 19, 0xFF408C3C);
                g.fill(x + 13, y + 23, x + 16, y + 26, 0xFF408C3C);
            }
        }
    }
}
