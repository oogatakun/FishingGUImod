package com.example.fishinggui.client;

import com.example.fishinggui.net.ModNetwork;
import com.example.fishinggui.net.SkillBuyPacket;
import com.example.fishinggui.net.SkillSyncPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Lists the skills with their level and a Learn button; the server decides every purchase. */
public class SkillScreen extends Screen {
    private static final int ROW_H = 36;
    private static final int ROW_W = 280;
    private static final int BTN_W = 90;

    public SkillScreen() {
        super(Component.translatable("gui.fishinggui.skills.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public void refresh() {
        rebuildWidgets();
    }

    private int top() {
        return 50;
    }

    @Override
    protected void init() {
        int left = (this.width - ROW_W) / 2;
        int y = top();
        for (SkillSyncPacket.Entry e : ClientSkills.skills()) {
            int btnX = left + ROW_W - BTN_W;
            if (e.level() >= e.maxLevel()) {
                Button max = Button.builder(Component.translatable("gui.fishinggui.skills.max"), b -> { })
                        .bounds(btnX, y + 8, BTN_W, 20).build();
                max.active = false;
                addRenderableWidget(max);
            } else {
                int cost = e.cost().get(e.level());
                Button buy = Button.builder(Component.translatable("gui.fishinggui.skills.buy", cost),
                        b -> ModNetwork.CHANNEL.sendToServer(new SkillBuyPacket(e.id())))
                        .bounds(btnX, y + 8, BTN_W, 20).build();
                buy.active = ClientSkills.points() >= cost;
                addRenderableWidget(buy);
            }
            y += ROW_H;
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        int cx = this.width / 2;
        int left = (this.width - ROW_W) / 2;
        g.drawCenteredString(this.font, this.title, cx, 16, 0xFFFFFF);
        g.drawCenteredString(this.font, Component.translatable("gui.fishinggui.skills.points", ClientSkills.points()),
                cx, 32, 0xFFFF55);
        int y = top();
        for (SkillSyncPacket.Entry e : ClientSkills.skills()) {
            g.drawString(this.font, Component.translatable("skill.fishinggui." + e.id()), left, y + 4, 0xFFFFFF);
            g.drawString(this.font, "Lv." + e.level() + "/" + e.maxLevel(), left + 120, y + 4, 0x55FF55);
            g.drawString(this.font, Component.translatable("skill.fishinggui." + e.id() + ".desc"), left, y + 18,
                    0xAAAAAA);
            y += ROW_H;
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (SkillKey.OPEN.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
