package com.example.fishinggui.client;

import com.example.fishinggui.fight.Rarity;
import com.example.fishinggui.net.CatchChoicePacket;
import com.example.fishinggui.net.ModNetwork;
import java.util.Locale;
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

        // item (8x)
        g.pose().pushPose();
        g.pose().translate(cx - 64, this.height / 2 - 56, 0);
        g.pose().scale(8f, 8f, 1f);
        g.renderItem(stack, 0, 0);
        g.pose().popPose();

        // stats box
        int boxW = 190;
        int boxY = this.height - 90;
        int boxX = cx - boxW / 2;
        g.fill(boxX, boxY, boxX + boxW, boxY + 36, 0x99000000);
        g.renderOutline(boxX, boxY, boxW, 36, 0xFFFFFFFF);
        drawStat(g, cx - boxW / 4, boxY + 6, Component.translatable("gui.fishinggui.catch.weight"),
                String.format(Locale.ROOT, "%.1f kg", weight));
        drawStat(g, cx + boxW / 4, boxY + 6, Component.translatable("gui.fishinggui.catch.xp"), "+" + xp);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawStat(GuiGraphics g, int x, int y, Component label, String value) {
        g.drawCenteredString(this.font, label, x, y, 0xAAAAAA);
        g.drawCenteredString(this.font, Component.literal(value), x, y + 14, 0xFFFFFF);
    }
}
