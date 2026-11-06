package com.example.fishinggui.client;

import com.example.fishinggui.block.TackleBenchMenu;
import com.example.fishinggui.item.ModItems;
import com.example.fishinggui.tackle.Part;
import com.example.fishinggui.tackle.RodSetup;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The tackle bench: a plank workbench with a leather mat for the parts and a read-out of what the rod will do. */
public class TackleBenchScreen extends AbstractContainerScreen<TackleBenchMenu> {
    private static final int FRAME = 0xFF1E140D;
    private static final int EDGE = 0xFF7A5634;
    private static final int PLANK_A = 0xFF8C6439;
    private static final int PLANK_B = 0xFF82592F;
    private static final int SEAM = 0xFF5A3D20;
    private static final int GRAIN = 0xFF724D29;
    private static final int MAT = 0xFF33261D;
    private static final int STITCH = 0xFF6F5A44;
    private static final int SLOT_BG = 0xFF2A211B;
    private static final int SLOT_EDGE_DARK = 0xFF120D09;
    private static final int SLOT_EDGE_LIGHT = 0xFF7B6650;
    private static final int PANEL = 0xFF211812;
    private static final int GOLD = 0xFFE8C468;
    private static final int TEXT = 0xFFD8CBB8;
    private static final int GOOD = 0xFF8FD27A;
    private static final int BAD = 0xFFE0705E;

    public TackleBenchScreen(TackleBenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = TackleBenchMenu.WIDTH;
        imageHeight = TackleBenchMenu.HEIGHT;
        titleLabelX = 14;
        titleLabelY = 8;
        inventoryLabelX = (TackleBenchMenu.WIDTH - 162) / 2;
        inventoryLabelY = TackleBenchMenu.INV_Y - 11;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, GOLD, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        int toolsY = TackleBenchMenu.toolsY();
        g.drawString(font, Component.translatable("gui.fishinggui.bench.disassemble"), TackleBenchMenu.disassembleX(),
                toolsY - 10, TEXT, false);
        Component made = Component.translatable("gui.fishinggui.bench.result");
        g.drawString(font, made, TackleBenchMenu.resultX() + 18 - font.width(made), toolsY - 10, TEXT, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        int w = imageWidth;
        int h = imageHeight;
        // frame and planks
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, FRAME);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, EDGE);
        g.fill(x, y, x + w, y + h, PLANK_A);
        for (int row = 0; row * 12 < h; row++) {
            int py = y + row * 12;
            if (row % 2 == 1) {
                g.fill(x, py, x + w, Math.min(y + h, py + 12), PLANK_B);
            }
            g.fill(x, py, x + w, py + 1, SEAM);
            for (int i = 0; i < 6; i++) {
                int gx = x + (row * 37 + i * 41) % (w - 14) + 4;
                g.fill(gx, py + 3 + (i * 3 + row) % 6, gx + 9 + (i * 5) % 8, py + 4 + (i * 3 + row) % 6, GRAIN);
            }
        }
        // the leather mat the parts lie on, with a stitched edge
        int matX = x + 6;
        int matY = y + 18;
        int matW = 106;
        int matH = TackleBenchMenu.toolsY() + 26 - 18;
        g.fill(matX - 1, matY - 1, matX + matW + 1, matY + matH + 1, FRAME);
        g.fill(matX, matY, matX + matW, matY + matH, MAT);
        stitch(g, matX + 3, matY + 3, matW - 6, matH - 6);
        // read-out panel
        int px = x + 116;
        int py = y + 18;
        int pw = w - 116 - 8;
        int ph = TackleBenchMenu.toolsY() + 26 - 18;
        g.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, FRAME);
        g.fill(px, py, px + pw, py + ph, PANEL);
        g.fill(px, py, px + pw, py + 1, SLOT_EDGE_LIGHT);
        g.fill(px, py + 12, px + pw, py + 13, EDGE);
        drawStats(g, px + 5, py + 3, pw - 10);
        // inventory plate
        int ix = x + (w - 162) / 2 - 4;
        int iy = y + TackleBenchMenu.INV_Y - 14;
        g.fill(ix - 1, iy - 1, ix + 170, iy + 94, FRAME);
        g.fill(ix, iy, ix + 169, iy + 93, 0xFF5C4430);
        for (Slot slot : menu.slots) {
            drawSlot(g, x + slot.x, y + slot.y);
        }
        drawPartSlots(g, x, y);
        drawArrow(g, x, y);
    }

    private static void stitch(GuiGraphics g, int x, int y, int w, int h) {
        for (int i = 0; i < w; i += 4) {
            g.fill(x + i, y, x + i + 2, y + 1, STITCH);
            g.fill(x + i, y + h - 1, x + i + 2, y + h, STITCH);
        }
        for (int i = 0; i < h; i += 4) {
            g.fill(x, y + i, x + 1, y + i + 2, STITCH);
            g.fill(x + w - 1, y + i, x + w, y + i + 2, STITCH);
        }
    }

    private static void drawSlot(GuiGraphics g, int sx, int sy) {
        g.fill(sx - 1, sy - 1, sx + 17, sy + 17, SLOT_EDGE_DARK);
        g.fill(sx, sy, sx + 17, sy + 17, SLOT_EDGE_LIGHT);
        g.fill(sx, sy, sx + 16, sy + 16, SLOT_BG);
        g.fill(sx, sy, sx + 16, sy + 1, 0xFF18120E);
        g.fill(sx, sy, sx + 1, sy + 16, 0xFF18120E);
    }

    /** A faded picture of what belongs in each empty slot, and a coloured rim: red if it is still required. */
    private void drawPartSlots(GuiGraphics g, int x, int y) {
        List<Part> parts = TackleBenchMenu.PARTS;
        RodSetup setup = menu.setup();
        for (int i = 0; i < parts.size(); i++) {
            Part part = parts.get(i);
            int sx = x + TackleBenchMenu.partX(i);
            int sy = y + TackleBenchMenu.partY(i);
            boolean filled = setup.rank(part) > 0;
            if (!filled) {
                g.renderFakeItem(new ItemStack(ModItems.PARTS.get(part).get(0).get()), sx, sy);
                g.fill(sx, sy, sx + 16, sy + 16, 0xCC2A211B);
            }
            int rim = filled ? 0xFF5FA05A : part.required() ? 0xFFB5483A : 0xFF6F5A44;
            g.fill(sx - 2, sy - 2, sx + 18, sy - 1, rim);
            g.fill(sx - 2, sy + 17, sx + 18, sy + 18, rim);
            g.fill(sx - 2, sy - 1, sx - 1, sy + 17, rim);
            g.fill(sx + 17, sy - 1, sx + 18, sy + 17, rim);
        }
        int dx = x + TackleBenchMenu.disassembleX();
        int dy = y + TackleBenchMenu.toolsY();
        if (menu.getSlot(TackleBenchMenu.PARTS.size()).getItem().isEmpty()) {
            g.renderFakeItem(new ItemStack(ModItems.TACKLE_ROD.get()), dx, dy);
            g.fill(dx, dy, dx + 16, dy + 16, 0xCC2A211B);
        }
        boolean ready = !menu.getSlot(TackleBenchMenu.PARTS.size() + 1).getItem().isEmpty();
        int rx = x + TackleBenchMenu.resultX();
        int ry = y + TackleBenchMenu.toolsY();
        int rim = ready ? GOLD : 0xFF6F5A44;
        g.fill(rx - 2, ry - 2, rx + 18, ry - 1, rim);
        g.fill(rx - 2, ry + 17, rx + 18, ry + 18, rim);
        g.fill(rx - 2, ry - 1, rx - 1, ry + 17, rim);
        g.fill(rx + 17, ry - 1, rx + 18, ry + 17, rim);
    }

    private void drawArrow(GuiGraphics g, int x, int y) {
        boolean ready = !menu.getSlot(TackleBenchMenu.PARTS.size() + 1).getItem().isEmpty();
        int color = ready ? GOLD : 0xFF6F5A44;
        int ax = x + TackleBenchMenu.resultX() - 30;
        int ay = y + TackleBenchMenu.toolsY() + 8;
        g.fill(ax, ay - 1, ax + 16, ay + 2, color);
        for (int i = 0; i < 4; i++) {
            g.fill(ax + 16 + i, ay - 4 + i, ax + 17 + i, ay + 5 - i, color);
        }
    }

    private void drawStats(GuiGraphics g, int x, int y, int width) {
        RodSetup setup = menu.setup();
        g.drawString(font, Component.translatable("gui.fishinggui.bench.stats"), x, y, GOLD, false);
        int ty = y + 14;
        boolean rod = setup.rank(Part.ROD_BLANK) > 0;
        row(g, x, ty, width, "level_cap", rod ? "Lv." + setup.levelCap() : "-", rod);
        row(g, x, ty + 10, width, "durability", rod ? String.valueOf(setup.durability()) : "-", rod);
        boolean reel = setup.rank(Part.REEL) > 0;
        row(g, x, ty + 20, width, "reel", reel ? mult(setup.reelSpeed()) : "-", reel);
        boolean line = setup.rank(Part.LINE) > 0;
        row(g, x, ty + 30, width, "tension", line ? mult(setup.tensionScale()) : "-", line);
        boolean hook = setup.rank(Part.HOOK) > 0;
        row(g, x, ty + 40, width, "luck", hook ? "+" + (int) setup.hookLuck() : "-", hook);
        List<Component> missing = new ArrayList<>();
        for (Part part : TackleBenchMenu.PARTS) {
            if (part.required() && setup.rank(part) == 0) {
                missing.add(Component.translatable("gui.fishinggui.bench.part." + part.id()));
            }
        }
        int my = ty + 54;
        if (missing.isEmpty()) {
            g.drawString(font, Component.translatable("gui.fishinggui.bench.ready"), x, my, GOOD, false);
        } else {
            g.drawString(font, Component.translatable("gui.fishinggui.bench.missing"), x, my, BAD, false);
            for (int i = 0; i < missing.size(); i++) {
                g.drawString(font, missing.get(i), x + 4, my + 10 + i * 10, BAD, false);
            }
        }
    }

    private void row(GuiGraphics g, int x, int y, int width, String key, String value, boolean on) {
        g.drawString(font, Component.translatable("gui.fishinggui.bench." + key), x, y, on ? TEXT : 0xFF8A7B6A, false);
        g.drawString(font, value, x + width - font.width(value), y, on ? GOOD : 0xFF8A7B6A, false);
    }

    private static String mult(double v) {
        return String.format(Locale.ROOT, "x%.2f", v);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (hoveredSlot != null && !hoveredSlot.hasItem()) {
            int i = hoveredSlot.index;
            Component text = null;
            if (i < TackleBenchMenu.PARTS.size()) {
                Part part = TackleBenchMenu.PARTS.get(i);
                text = Component.translatable("gui.fishinggui.bench.part." + part.id());
                if (part.required()) {
                    text = text.copy().append(Component.translatable("gui.fishinggui.bench.required"));
                }
            } else if (i == TackleBenchMenu.PARTS.size()) {
                text = Component.translatable("gui.fishinggui.bench.disassemble.tip");
            }
            if (text != null) {
                g.renderTooltip(font, text, mouseX, mouseY);
                return;
            }
        }
        super.renderTooltip(g, mouseX, mouseY);
    }
}
