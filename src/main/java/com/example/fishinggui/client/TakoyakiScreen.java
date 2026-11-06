package com.example.fishinggui.client;

import static com.example.fishinggui.food.TakoyakiRules.*;

import com.example.fishinggui.block.TakoyakiPanBlock;
import com.example.fishinggui.block.TakoyakiPanBlockEntity;
import com.example.fishinggui.block.TakoyakiService;
import com.example.fishinggui.net.ModNetwork;
import com.example.fishinggui.net.TakoyakiActionPacket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The pan: pick a tool in the bar (it follows the cursor), then click wells one at a time. The server decides every
 * action; the pan is drawn from plain shapes so it needs no textures.
 */
public class TakoyakiScreen extends Screen {
    private static final int CELL = 36;
    private static final int GAP = 8;
    private static final int COLS = 4;
    private static final int ROWS = 3;
    private static final int TOOL = 26;
    private static final int GRID_W = COLS * CELL + (COLS - 1) * GAP;
    private static final int GRID_H = ROWS * CELL + (ROWS - 1) * GAP;
    private static final int FRAME = 12;
    private static final int HANDLE = 16;
    private static final int TITLE_H = 24;
    private static final int TOTAL_H = TITLE_H + FRAME + GRID_H + 14 + 10 + TOOL + 6 + 14 + 20 + 8;

    private final BlockPos pos;
    private int tool = -1;
    /** Wells already poured or sprinkled during the current drag. */
    private final Set<Integer> dragged = new HashSet<>();
    /** Wells chosen with the bowl, sent as one scoop on release. */
    private int pickedMask;
    /** The well being stirred with the ice pick, and how far round (radians) the cursor has gone. */
    private int spinWell = -1;
    private double spinLast;
    private double spinAccum;
    /** Short-lived marks left by the tool as it is dragged: x, y, age in ticks. */
    private final List<int[]> trail = new ArrayList<>();

    public TakoyakiScreen(BlockPos pos) {
        super(Component.translatable("block.fishinggui.takoyaki_pan"));
        this.pos = pos;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int panelW() {
        return GRID_W + 2 * (FRAME + HANDLE) + 8;
    }

    private int left() {
        return (width - panelW()) / 2;
    }

    private int top() {
        return (height - TOTAL_H) / 2;
    }

    private int frameTop() {
        return top() + TITLE_H;
    }

    private int gridLeft() {
        return (width - GRID_W) / 2;
    }

    private int gridTop() {
        return frameTop() + FRAME;
    }

    private int frameBottom() {
        return gridTop() + GRID_H + 14;
    }

    private int toolLeft() {
        return (width - TOOLS * TOOL) / 2;
    }

    private int toolTop() {
        return frameBottom() + 10;
    }

    private int closeTop() {
        return toolTop() + TOOL + 6 + 14;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.fishinggui.takoyaki.close"), b -> onClose())
                .bounds(width / 2 - 40, closeTop(), 80, 20).build());
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
        trail.removeIf(t -> ++t[2] > 24);
        var player = Minecraft.getInstance().player;
        if (pan() == null || player == null
                || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64) {
            onClose();
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) {
            return true;
        }
        if (button == 1) {
            tool = -1;
            return true;
        }
        for (int t = 0; t < TOOLS; t++) {
            if (inside(mx, my, toolLeft() + t * TOOL, toolTop(), TOOL - 2, TOOL - 2)) {
                tool = tool == t ? -1 : t;
                return true;
            }
        }
        int well = wellAt(mx, my);
        if (well >= 0 && tool >= 0) {
            dragged.clear();
            pickedMask = 0;
            spinWell = -1;
            beginOn(well, mx, my);
            return true;
        }
        return well >= 0;
    }

    private int wellAt(double mx, double my) {
        for (int well = 0; well < WELLS; well++) {
            if (inside(mx, my, cellX(well), cellY(well), CELL, CELL)) {
                return well;
            }
        }
        return -1;
    }

    private void send(int mask) {
        ModNetwork.CHANNEL.sendToServer(new TakoyakiActionPacket(pos, tool, mask));
    }

    private void beginOn(int well, double mx, double my) {
        TakoyakiPanBlockEntity be = pan();
        if (be == null) {
            return;
        }
        if (tool == TOOL_PICK) {
            if (be.stage(well) == FLIP) {
                spinWell = well;
                spinLast = angle(well, mx, my);
                spinAccum = 0;
            } else {
                send(1 << well);
            }
        } else if (tool == TOOL_BOWL) {
            tryPick(be, well);
        } else {
            hit(well);
        }
    }

    private void hit(int well) {
        if (dragged.add(well)) {
            send(1 << well);
        }
    }

    private void tryPick(TakoyakiPanBlockEntity be, int well) {
        if ((pickedMask >> well & 1) == 0 && be.stage(well) == READY && be.toppings(well) != 0
                && Integer.bitCount(pickedMask) < PORTIONS) {
            pickedMask |= 1 << well;
            if (Integer.bitCount(pickedMask) == PORTIONS) {
                send(pickedMask);
                pickedMask = 0;
            }
        }
    }

    private double angle(int well, double mx, double my) {
        return Math.atan2(my - (cellY(well) + CELL / 2.0), mx - (cellX(well) + CELL / 2.0));
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button != 0 || tool < 0) {
            return super.mouseDragged(mx, my, button, dx, dy);
        }
        TakoyakiPanBlockEntity be = pan();
        int well = wellAt(mx, my);
        if (be != null && tool == TOOL_PICK && spinWell >= 0) {
            double cx = cellX(spinWell) + CELL / 2.0;
            double cy = cellY(spinWell) + CELL / 2.0;
            if (Math.hypot(mx - cx, my - cy) > 4) {
                double now = angle(spinWell, mx, my);
                double delta = now - spinLast;
                if (delta > Math.PI) {
                    delta -= 2 * Math.PI;
                } else if (delta < -Math.PI) {
                    delta += 2 * Math.PI;
                }
                spinAccum += Math.abs(delta);
                spinLast = now;
                if (spinAccum >= 2 * Math.PI) {
                    send(1 << spinWell);
                    spinWell = -1;
                }
            }
        } else if (be != null && well >= 0 && tool == TOOL_BOWL) {
            tryPick(be, well);
        } else if (well >= 0 && tool != TOOL_PICK) {
            hit(well);
        }
        if (well >= 0 && tool != TOOL_PICK && tool != TOOL_BOWL && trail.size() < 80) {
            trail.add(new int[] {(int) mx, (int) my, 0});
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (tool == TOOL_BOWL && pickedMask != 0) {
            send(pickedMask);
        }
        pickedMask = 0;
        spinWell = -1;
        dragged.clear();
        return super.mouseReleased(mx, my, button);
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
        g.fill(left(), top(), left() + panelW(), top() + TOTAL_H, 0xE0181818);
        g.drawCenteredString(font, title, width / 2, top() + 8, 0xFFFFFF);
        TakoyakiPanBlockEntity be = pan();
        if (be == null) {
            return;
        }
        boolean hot = be.getBlockState().getValue(TakoyakiPanBlock.POWERED);
        drawPan(g, hot);
        Component hovered = null;
        for (int well = 0; well < WELLS; well++) {
            drawWell(g, be, well, hot);
            if (inside(mouseX, mouseY, cellX(well), cellY(well), CELL, CELL)) {
                hovered = Component.translatable("gui.fishinggui.takoyaki.stage." + be.stage(well));
            }
        }
        ItemStack hoveredTool = ItemStack.EMPTY;
        for (int t = 0; t < TOOLS; t++) {
            int x = toolLeft() + t * TOOL;
            int y = toolTop();
            boolean ok = usable(t);
            g.fill(x, y, x + TOOL - 2, y + TOOL - 2, t == tool ? 0xFFFFE070 : 0xFF8B8B8B);
            g.fill(x + 1, y + 1, x + TOOL - 3, y + TOOL - 3, 0xFF373737);
            g.fill(x + 1, y + 1, x + TOOL - 4, y + 2, 0xFF1F1F1F);
            ItemStack shown = findTool(t);
            if (shown.isEmpty()) {
                shown = new ItemStack(TakoyakiService.toolItem(t));
            }
            g.renderItem(shown, x + 3, y + 3);
            if (ok) {
                g.renderItemDecorations(font, shown, x + 3, y + 3);
            } else {
                g.fill(x + 1, y + 1, x + TOOL - 3, y + TOOL - 3, 0xAA000000);
            }
            if (inside(mouseX, mouseY, x, y, TOOL - 2, TOOL - 2)) {
                hoveredTool = shown;
            }
        }
        Component hint = hot ? Component.translatable("gui.fishinggui.takoyaki.hint")
                : Component.translatable("gui.fishinggui.takoyaki.off");
        g.drawCenteredString(font, hint, width / 2, toolTop() + TOOL + 6, 0xAAAAAA);
        super.render(g, mouseX, mouseY, partialTick);
        if (!hoveredTool.isEmpty()) {
            g.renderTooltip(font, hoveredTool, mouseX, mouseY);
        } else if (hovered != null) {
            g.renderTooltip(font, hovered, mouseX, mouseY + (tool >= 0 ? 18 : 0));
        }
        drawTrail(g);
        if (tool >= 0) {
            ItemStack held = findTool(tool);
            if (held.isEmpty()) {
                held = new ItemStack(TakoyakiService.toolItem(tool));
            }
            g.pose().pushPose();
            g.pose().translate(0, 0, 400);
            g.renderItem(held, mouseX - 8, mouseY - 8);
            if (tool == TOOL_BOWL && pickedMask != 0) {
                g.drawString(font, "x" + Integer.bitCount(pickedMask), mouseX + 8, mouseY + 2, 0xFFFFE070);
            }
            g.pose().popPose();
        }
    }

    /** The cast-iron plate with its two handles; it glows a little while it is heating. */
    private void drawPan(GuiGraphics g, boolean hot) {
        int x0 = gridLeft() - FRAME;
        int x1 = gridLeft() + GRID_W + FRAME;
        int y0 = frameTop();
        int y1 = frameBottom();
        int hy = (y0 + y1) / 2;
        g.fill(x0 - HANDLE, hy - 4, x0, hy + 4, 0xFF5A3A22);
        g.fill(x0 - HANDLE, hy - 4, x0, hy - 2, 0xFF7A5232);
        g.fill(x1, hy - 4, x1 + HANDLE, hy + 4, 0xFF5A3A22);
        g.fill(x1, hy - 4, x1 + HANDLE, hy - 2, 0xFF7A5232);
        g.fill(x0 - 2, y0 - 2, x1 + 2, y1 + 2, 0xFF0E0E0E);
        g.fillGradient(x0, y0, x1, y1, 0xFF4A4A4F, hot ? 0xFF3A2A26 : 0xFF2E2E32);
        g.fill(x0, y0, x1, y0 + 1, 0xFF8A8A92);
        g.fill(x0, y0, x0 + 1, y1, 0xFF70707A);
        g.fill(x0, y1 - 1, x1, y1, 0xFF1A1A1E);
        g.fill(x1 - 1, y0, x1, y1, 0xFF1A1A1E);
    }

    private void drawWell(GuiGraphics g, TakoyakiPanBlockEntity be, int well, boolean hot) {
        int x = cellX(well);
        int y = cellY(well);
        int cx = x + CELL / 2;
        int cy = y + CELL / 2;
        int stage = be.stage(well);
        if (stage == FLIP && System.currentTimeMillis() / 300 % 2 == 0) {
            circle(g, cx, cy, 19, 0xFFFFD34D);
        }
        circle(g, cx, cy, 18, 0xFF8A8A92);
        circle(g, cx, cy, 17, 0xFF111114);
        circle(g, cx, cy, 15, hot ? 0xFF2A1C18 : 0xFF1C1C20);
        circle(g, cx - 1, cy - 1, 12, hot ? 0xFF33221C : 0xFF222226);
        if (well == spinWell) {
            int lit = (int) (spinAccum / (2 * Math.PI) * 12);
            for (int i = 0; i < 12; i++) {
                double a = i * Math.PI / 6 - Math.PI / 2;
                int px = cx + (int) Math.round(Math.cos(a) * 22);
                int py = cy + (int) Math.round(Math.sin(a) * 22);
                g.fill(px - 1, py - 1, px + 2, py + 2, i < lit ? 0xFFFFE070 : 0xFF55555A);
            }
        }
        if (stage == EMPTY || (pickedMask >> well & 1) != 0) {
            return;
        }
        if (stage == BATTER || stage == FILLED) {
            circle(g, cx, cy, 14, 0xFFD9C98C);
            circle(g, cx, cy, 13, 0xFFF5E8B0);
            circle(g, cx - 4, cy - 4, 3, 0xFFFFF8D8);
            if (stage == FILLED) {
                circle(g, cx + 1, cy + 1, 5, 0xFFC27880);
                circle(g, cx, cy, 4, 0xFFE29AA0);
                circle(g, cx - 1, cy - 1, 1, 0xFFF6C4C8);
            }
            return;
        }
        int[] rgb = color(stage, be.progress(well));
        circle(g, cx, cy, 14, argb(shade(rgb, 0.65f)));
        circle(g, cx, cy, 13, argb(rgb));
        circle(g, cx - 3, cy - 3, 6, argb(shade(rgb, 1.08f)));
        circle(g, cx - 5, cy - 5, 2, argb(shade(rgb, 1.2f)));
        if (isCooking(stage)) {
            int w = Math.round(24 * be.progress(well));
            g.fill(cx - 12, y + CELL + 2, cx + 12, y + CELL + 5, 0xFF000000);
            g.fill(cx - 12, y + CELL + 2, cx - 12 + w, y + CELL + 5, 0xFFFF9A3C);
        }
        if (stage == READY) {
            drawToppings(g, cx, cy, be.toppings(well));
        }
    }

    private void drawTrail(GuiGraphics g) {
        int color = switch (tool) {
            case TOOL_MAYO -> 0xFCF8E4;
            case TOOL_AONORI -> 0x3F8C3A;
            case TOOL_OCTOPUS -> 0xE29AA0;
            default -> 0xF5E8B0;
        };
        for (int[] t : trail) {
            int alpha = Math.max(0, 255 - t[2] * 10);
            int size = tool == TOOL_AONORI ? 2 : 3;
            g.fill(t[0] - 1, t[1] - 1, t[0] - 1 + size, t[1] - 1 + size, alpha << 24 | color);
        }
    }

    private static void drawToppings(GuiGraphics g, int cx, int cy, int toppings) {
        if ((toppings & MAYO) != 0) {
            for (int row = 0; row < 2; row++) {
                for (int i = -8; i <= 8; i += 2) {
                    int py = cy - 4 + row * 7 + ((i / 2) % 2 == 0 ? 0 : 2);
                    g.fill(cx + i, py, cx + i + 2, py + 2, 0xFFFCF8E4);
                }
            }
        }
        if ((toppings & AONORI) != 0) {
            int[][] specks = {{-7, -6}, {4, -8}, {-2, -1}, {7, 2}, {-8, 5}, {1, 7}, {-4, 9}};
            for (int[] s : specks) {
                g.fill(cx + s[0], cy + s[1], cx + s[0] + 2, cy + s[1] + 2, 0xFF3F8C3A);
            }
        }
    }

    private static void circle(GuiGraphics g, int cx, int cy, int r, int argb) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.round(Math.sqrt((double) r * r - (double) dy * dy));
            g.fill(cx - dx, cy + dy, cx + dx, cy + dy + 1, argb);
        }
    }

    private static int[] shade(int[] rgb, float f) {
        return new int[] {Math.min(255, Math.round(rgb[0] * f)), Math.min(255, Math.round(rgb[1] * f)),
                Math.min(255, Math.round(rgb[2] * f))};
    }

    private static int argb(int[] rgb) {
        return 0xFF000000 | rgb[0] << 16 | rgb[1] << 8 | rgb[2];
    }
}
