package com.example.fishinggui.client;

import com.example.fishinggui.fight.Rarity;
import com.example.fishinggui.net.CatchChoicePacket;
import com.example.fishinggui.net.ModNetwork;
import com.example.fishinggui.item.FishLevels;
import java.util.Locale;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Shown after a successful fight: name, rarity, weight, XP and Keep / Release. ESC counts as Keep. */
public class CatchScreen extends Screen {
    private static final int[] RARITY_COLORS = {0xFFFFFFFF, 0xFF55FF55, 0xFF55AAFF, 0xFFAA55FF};
    private static final int BTN_W = 100;
    private static final int BTN_H = 20;

    private final ItemStack stack;
    private final Rarity rarity;
    private final float weight;
    private final int xp;
    private boolean chosen;

    public CatchScreen(ItemStack stack, Rarity rarity, float weight, int xp) {
        super(Component.literal("Catch"));
        this.stack = stack;
        this.rarity = rarity;
        this.weight = weight;
        this.xp = xp;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height - 44;
        addRenderableWidget(Button.builder(Component.translatable("gui.fishinggui.catch.keep"), b -> choose(true))
                .bounds(cx - BTN_W - 6, y, BTN_W, BTN_H).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.fishinggui.catch.release"), b -> choose(false))
                .bounds(cx + 6, y, BTN_W, BTN_H).build());
    }

    private void choose(boolean keep) {
        if (chosen) {
            return;
        }
        chosen = true;
        ModNetwork.CHANNEL.sendToServer(new CatchChoicePacket(keep));
        onClose();
    }

    @Override
    public void removed() {
        if (!chosen) {
            chosen = true;
            if (Minecraft.getInstance().getConnection() != null) {
                ModNetwork.CHANNEL.sendToServer(new CatchChoicePacket(true));
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int cx = this.width / 2;
        int rarityColor = RARITY_COLORS[Math.min(rarity.ordinal(), RARITY_COLORS.length - 1)];

        // name (3x)
        g.pose().pushPose();
        g.pose().scale(3f, 3f, 1f);
        g.drawCenteredString(this.font, stack.getHoverName(), cx / 3, (this.height / 8) / 3, 0xFFFFFF);
        g.pose().popPose();

        // rarity badge
        Component badge = Component.translatable(rarity.langKey());
        int bw = this.font.width(badge) + 24;
        int bx = cx - bw / 2;
        int by = this.height / 8 + 34;
        g.fill(bx, by, bx + bw, by + 16, 0x99000000);
        g.renderOutline(bx, by, bw, 16, rarityColor);
        g.drawCenteredString(this.font, badge, cx, by + 4, rarityColor);
        Integer fishLevel = FishLevels.BY_ID.get(String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem())));
        if (fishLevel != null) {
            Component lv = Component.translatable("tooltip.fishinggui.fish_level", fishLevel);
            int lw = this.font.width(lv) + 24;
            g.fill(cx + bw / 2 + 6, by, cx + bw / 2 + 6 + lw, by + 16, 0x99000000);
            g.renderOutline(cx + bw / 2 + 6, by, lw, 16, 0xFF55FFFF);
            g.drawCenteredString(this.font, lv, cx + bw / 2 + 6 + lw / 2, by + 4, 0xFF55FFFF);
        }

        // item: up to 8x, shrunk to fit between the badge and the stats box
        int boxY = this.height - 110;
        int top = by + 16 + 6;
        int avail = boxY - 6 - top;
        float scale = Math.max(1f, Math.min(8f, avail / 16));
        int size = (int) (16 * scale);
        g.pose().pushPose();
        g.pose().translate(cx - size / 2f, top + (avail - size) / 2f, 0);
        g.pose().scale(scale, scale, 1f);
        g.renderItem(stack, 0, 0);
        g.pose().popPose();

        // stats box
        int boxW = 190;
        int boxH = 56;
        int boxX = cx - boxW / 2;
        g.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0x99000000);
        g.renderOutline(boxX, boxY, boxW, boxH, 0xFFFFFFFF);
        drawStat(g, cx - boxW / 4, boxY + 6, Component.translatable("gui.fishinggui.catch.weight"),
                String.format(Locale.ROOT, "%.1f kg", weight));
        drawStat(g, cx + boxW / 4, boxY + 6, Component.translatable("gui.fishinggui.catch.xp"), "+" + xp);
        drawLevelBar(g, boxX + 8, boxY + 38, boxW - 16);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawLevelBar(GuiGraphics g, int x, int y, int w) {
        int need = ClientProgress.xpToNext();
        float frac = need <= 0 ? 1f : Math.min(1f, ClientProgress.xp() / (float) need);
        String text = need <= 0
                ? Component.translatable("gui.fishinggui.level", ClientProgress.level()).getString() + " MAX"
                : Component.translatable("gui.fishinggui.level", ClientProgress.level()).getString()
                        + "  " + ClientProgress.xp() + "/" + need;
        g.drawString(this.font, text, x, y - 10, 0xFFFFFF, false);
        g.fill(x, y, x + w, y + 6, 0xFF222222);
        g.fill(x, y, x + (int) (w * frac), y + 6, 0xFF55FF55);
        g.renderOutline(x - 1, y - 1, w + 2, 8, 0xFFFFFFFF);
    }

    private void drawStat(GuiGraphics g, int x, int y, Component label, String value) {
        g.drawCenteredString(this.font, label, x, y, 0xAAAAAA);
        g.drawCenteredString(this.font, Component.literal(value), x, y + 14, 0xFFFFFF);
    }
}
