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
import org.lwjgl.glfw.GLFW;

/** Non-pausing tension gauge. Hold the right mouse button to reel. */
public class FightScreen extends Screen {
    private static final int KEEP_ALIVE_TICKS = 10;
    private static final int GAUGE_W = 16;
    private static final int GAUGE_H = 120;
    private static final int STAMINA_W = 100;

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
            playUi(SoundEvents.FISHING_BOBBER_SPLASH, 1.0f);
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
        int x = cx - GAUGE_W / 2 + shake;
        int top = cy - GAUGE_H / 2;
        int bottom = top + GAUGE_H;

        g.fill(x - 2, top - 2, x + GAUGE_W + 2, bottom + 2, 0xCC000000);
        int dangerTop = bottom - (int) (GAUGE_H * FightSession.DANGER_TENSION / FightSession.MAX_TENSION);
        g.fill(x, top, x + GAUGE_W, dangerTop, 0x66AA2222);
        int filled = (int) (GAUGE_H * tension / FightSession.MAX_TENSION);
        int color = tension >= FightSession.DANGER_TENSION ? 0xFFFF4444 : (dashing ? 0xFFFFAA00 : 0xFF44CC44);
        g.fill(x, bottom - filled, x + GAUGE_W, bottom, color);
        g.fill(x - 3, dangerTop, x + GAUGE_W + 3, dangerTop + 1, 0xFFFF8888);

        int barX = cx - STAMINA_W / 2;
        int barY = bottom + 10;
        g.fill(barX - 1, barY - 1, barX + STAMINA_W + 1, barY + 7, 0xCC000000);
        int staminaW = (int) (STAMINA_W * Math.max(0f, stamina) / maxStamina);
        g.fill(barX, barY, barX + staminaW, barY + 6, 0xFF4488FF);

        g.drawCenteredString(this.font, Component.literal((int) tension + " / 100"), cx, top - 14, 0xFFFFFF);
        if (telegraph) {
            g.drawCenteredString(this.font, Component.literal("!"), cx + GAUGE_W + 12, cy, 0xFFFF55);
        }
        g.drawCenteredString(this.font, Component.literal("Hold right-click to reel"), cx, barY + 12, 0xAAAAAA);
    }

    private static void playUi(net.minecraft.sounds.SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch));
    }
}
