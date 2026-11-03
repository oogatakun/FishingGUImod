package com.example.fishinggui.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/** Full-screen "LEVEL UP!" celebration: rays, sparkles and a fanfare, about three seconds. */
public final class LevelUpOverlay {
    private static final long DURATION_MS = 3000L;
    private static final int RAYS = 14;
    private static final int SPARKLES = 40;

    private static long startMs = -1L;
    private static int level;

    private LevelUpOverlay() {
    }

    public static void start(int newLevel) {
        level = newLevel;
        startMs = System.currentTimeMillis();
        var sounds = Minecraft.getInstance().getSoundManager();
        sounds.play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.0f, 1.0f));
        sounds.play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f));
    }

    public static void render(GuiGraphics g, int width, int height) {
        if (startMs < 0) {
            return;
        }
        long elapsed = System.currentTimeMillis() - startMs;
        if (elapsed >= DURATION_MS) {
            startMs = -1L;
            return;
        }
        float t = elapsed / (float) DURATION_MS;
        float fade = t < 0.08f ? t / 0.08f : (t > 0.8f ? (1f - t) / 0.2f : 1f);
        int alpha = Math.max(0, Math.min(255, (int) (fade * 255)));
        int cx = width / 2;
        int cy = height / 2 - 20;

        // soft dim behind the text
        g.fill(0, cy - 60, width, cy + 60, ((int) (fade * 110) << 24));

        // rotating rays
        float spin = elapsed / 20f;
        float len = Math.max(width, height) * (0.25f + 0.35f * Math.min(1f, t * 3f));
        for (int i = 0; i < RAYS; i++) {
            g.pose().pushPose();
            g.pose().translate(cx, cy, 0);
            g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(spin + i * (360f / RAYS)));
            int rayColor = ((int) (fade * 70) << 24) | (i % 2 == 0 ? 0xFFE066 : 0xFFFFFF);
            g.fill(0, -2, (int) len, 2, rayColor);
            g.pose().popPose();
        }

        // sparkles (deterministic positions that twinkle)
        for (int i = 0; i < SPARKLES; i++) {
            double a = i * 2.399963;
            double r = 40 + (i * 37 % 160) + 40 * t;
            int sx = cx + (int) (Math.cos(a) * r * 1.6);
            int sy = cy + (int) (Math.sin(a) * r * 0.7);
            float twinkle = 0.5f + 0.5f * (float) Math.sin(elapsed / 90.0 + i);
            int c = ((int) (fade * twinkle * 255) << 24) | 0xFFFFAA;
            g.fill(sx - 1, sy - 3, sx + 1, sy + 3, c);
            g.fill(sx - 3, sy - 1, sx + 3, sy + 1, c);
        }

        // title with a pop-in scale
        float pop = t < 0.15f ? 0.4f + 1.1f * (t / 0.15f) : (t < 0.25f ? 1.5f - 0.5f * ((t - 0.15f) / 0.1f) : 1.0f);
        drawCentered(g, Component.translatable("gui.fishinggui.levelup.title"), cx, cy - 22, 6f * pop,
                (alpha << 24) | 0xFFD23C);
        drawCentered(g, Component.translatable("gui.fishinggui.levelup.level", level), cx, cy + 22, 3.5f,
                (alpha << 24) | 0xFFFFFF);
    }

    private static void drawCentered(GuiGraphics g, Component text, int x, int y, float scale, int argb) {
        var font = Minecraft.getInstance().font;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1f);
        g.drawString(font, text, -font.width(text) / 2, -font.lineHeight / 2, argb, true);
        g.pose().popPose();
    }
}
