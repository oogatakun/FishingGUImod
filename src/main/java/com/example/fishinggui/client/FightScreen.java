package com.example.fishinggui.client;

import com.example.fishinggui.fight.FightSession;
import com.example.fishinggui.net.ModNetwork;
import com.example.fishinggui.net.ReelInputPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

/** Non-pausing tension gauge. Hold the right mouse button to reel. */
public class FightScreen extends Screen {
    private static final int KEEP_ALIVE_TICKS = 10;
    private static final int BAR_W = 9;
    private static final int BAR_H = 140;
    private static final int BAR_GAP = 6;
    private static final int LABEL_GAP = 6;
    private static final int PILL_H = 14;
    private static final int ICON_R = 17;

    private final float maxStamina;
    private float tension;
    private float stamina;
    private boolean telegraph;
    private boolean dashing;
    private boolean wasTelegraph;
    private boolean reeling;
    private boolean finished;
    private int tickCount;
    private int sinceSend;

    public FightScreen(float maxStamina) {
        super(Component.literal("Fishing"));
        this.maxStamina = maxStamina;
        this.stamina = maxStamina;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        tickCount++;
        long window = Minecraft.getInstance().getWindow().getWindow();
        boolean down = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (down != reeling || ++sinceSend >= KEEP_ALIVE_TICKS) {
            reeling = down;
            sinceSend = 0;
            ModNetwork.CHANNEL.sendToServer(new ReelInputPacket(down ? ReelInputPacket.REEL : ReelInputPacket.RELEASE));
        }
        if (tension >= FightSession.DANGER_TENSION && tickCount % 10 == 0) {
            playUi(SoundEvents.DISPENSER_FAIL, 1.5f);
        }
    }

    /** Applies a server state; any phase other than ACTIVE closes the screen. */
    public void apply(float tension, float stamina, boolean telegraph, boolean dashing, FightSession.Phase phase) {
        this.tension = tension;
        this.stamina = stamina;
        this.telegraph = telegraph;
        this.dashing = dashing;
        if (telegraph && !wasTelegraph) {
            playUi(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f);
        }
        wasTelegraph = telegraph;
        if (phase != FightSession.Phase.ACTIVE) {
            finished = true;
            onClose();
        }
    }

    @Override
    public void removed() {
        if (!finished) {
            finished = true;
            if (Minecraft.getInstance().getConnection() != null) {
                ModNetwork.CHANNEL.sendToServer(new ReelInputPacket(ReelInputPacket.ABORT));
            }
        }
        ClientFightHooks.clear(this);
    }

    /** Suppresses the ABORT on close (used when a newer fight replaces this screen). */
    void markFinished() {
        this.finished = true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int shake = telegraph ? ((tickCount & 1) == 0 ? 2 : -2) : 0;
        int top = cy - BAR_H / 2;
        int bottom = top + BAR_H;
        int leftX = cx - BAR_GAP / 2 - BAR_W + shake;
        int rightX = cx + BAR_GAP / 2 + shake;

        // icon
        int iconCy = top - 8;
        fillCircle(g, cx, iconCy, ICON_R, 0xDD000000);
        g.renderItem(new ItemStack(Items.FISHING_ROD), cx - 8, iconCy - 8);

        // left bar: landing progress
        float progress = GaugeState.progress(stamina, maxStamina);
        drawTrack(g, leftX, top, bottom);
        int progressH = (int) (BAR_H * progress);
        g.fill(leftX, bottom - progressH, leftX + BAR_W, bottom, 0xFFFFFFFF);
        g.fill(leftX - 1, bottom - progressH - 1, leftX + BAR_W + 1, bottom - progressH + 1, 0xFFFFFFFF);

        // right bar: tension
        drawTrack(g, rightX, top, bottom);
        int tensionH = (int) (BAR_H * Math.min(1f, tension / FightSession.MAX_TENSION));
        for (int i = 0; i < tensionH; i++) {
            g.fill(rightX, bottom - i - 1, rightX + BAR_W, bottom - i, tensionColor((float) i / BAR_H));
        }
        int dangerY = bottom - (int) (BAR_H * FightSession.DANGER_TENSION / FightSession.MAX_TENSION);
        g.fill(rightX - 2, dangerY, rightX + BAR_W + 2, dangerY + 1, 0x88FF6666);
        int tipColor = dashing ? 0xFFFFAA00 : 0xFFFFFFFF;
        g.fill(rightX - 2, bottom - tensionH - 1, rightX + BAR_W + 2, bottom - tensionH + 1, tipColor);

        // labels
        GaugeState.Progress p = GaugeState.progressLabel(progress);
        GaugeState.Line l = GaugeState.lineLabel(tension, FightSession.DANGER_TENSION);
        int[] ys = {top + 2, cy - PILL_H / 2, bottom - PILL_H - 2};
        int leftEdge = leftX - LABEL_GAP;
        int rightEdge = rightX + BAR_W + LABEL_GAP;
        drawLabel(g, "gui.fishinggui.progress.almost", leftEdge, ys[0], true, p == GaugeState.Progress.ALMOST);
        drawLabel(g, "gui.fishinggui.progress.battle", leftEdge, ys[1], true, p == GaugeState.Progress.BATTLE);
        drawLabel(g, "gui.fishinggui.progress.start", leftEdge, ys[2], true, p == GaugeState.Progress.START);
        drawLabel(g, "gui.fishinggui.line.danger", rightEdge, ys[0], false, l == GaugeState.Line.DANGER);
        drawLabel(g, "gui.fishinggui.line.safe", rightEdge, ys[1], false, l == GaugeState.Line.SAFE);
        drawLabel(g, "gui.fishinggui.line.slack", rightEdge, ys[2], false, l == GaugeState.Line.SLACK);

        if (telegraph) {
            g.drawCenteredString(this.font, Component.literal("!"), cx, bottom + 4, 0xFFFF55);
        }
        g.drawCenteredString(this.font, Component.translatable("gui.fishinggui.hint"), cx, bottom + 16, 0xAAAAAA);
    }

    private static void drawTrack(GuiGraphics g, int x, int top, int bottom) {
        g.fill(x - 1, top - 1, x + BAR_W + 1, bottom + 1, 0xCC000000);
        g.fill(x, top, x + BAR_W, bottom, 0xFF202020);
    }

    /** Green (low) -> yellow -> red (high) over the bar height. */
    private static int tensionColor(float f) {
        int r;
        int gr;
        if (f < 0.5f) {
            r = (int) (0x44 + (0xFF - 0x44) * (f / 0.5f));
            gr = 0xCC;
        } else {
            r = 0xFF;
            gr = (int) (0xCC - (0xCC - 0x44) * ((f - 0.5f) / 0.5f));
        }
        return 0xFF000000 | (r << 16) | (gr << 8) | 0x33;
    }

    private void drawLabel(GuiGraphics g, String key, int edge, int y, boolean alignRight, boolean active) {
        Component text = Component.translatable(key);
        String prefix = key.endsWith("line.safe") ? "◀▶ " : "";
        Component shown = prefix.isEmpty() ? text : Component.literal(prefix).append(text);
        int w = this.font.width(shown) + 10;
        int x1 = alignRight ? edge - w : edge;
        fillRounded(g, x1, y, x1 + w, y + PILL_H, active ? 0xCC000000 : 0x88000000);
        g.drawString(this.font, shown, x1 + 5, y + 3, active ? 0xFFFFFF : 0x777777, false);
    }

    private static void fillRounded(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1 + 1, y1, x2 - 1, y2, color);
        g.fill(x1, y1 + 1, x2, y2 - 1, color);
    }

    private static void fillCircle(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.sqrt((double) r * r - (double) dy * dy);
            g.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    private static void playUi(net.minecraft.sounds.SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch));
    }
}
