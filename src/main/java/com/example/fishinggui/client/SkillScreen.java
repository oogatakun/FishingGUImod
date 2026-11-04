package com.example.fishinggui.client;

import com.example.fishinggui.item.ModItems;
import com.example.fishinggui.net.ModNetwork;
import com.example.fishinggui.net.SkillResetPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import com.example.fishinggui.net.SkillBuyPacket;
import com.example.fishinggui.net.SkillSyncPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Lists the skills with their level and a Learn button; the server decides every purchase. */
public class SkillScreen extends Screen {
    private static final int ROW_H = 46;
    private static final int ROW_W = 420;
    private static final int BTN_W = 80;
    private static final int RESET_W = 60;
    private static final int GAP = 4;

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

    private static boolean holdsResetDrop() {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        Inventory inv = player.getInventory();
        for (ItemStack s : inv.items) {
            if (s.is(ModItems.RESET_DROP.get())) {
                return true;
            }
        }
        for (ItemStack s : inv.offhand) {
            if (s.is(ModItems.RESET_DROP.get())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void init() {
        int left = (this.width - ROW_W) / 2;
        int y = top();
        boolean hasDrop = holdsResetDrop();
        int btnX = left + ROW_W - RESET_W - GAP - BTN_W;
        int resetX = left + ROW_W - RESET_W;
        for (SkillSyncPacket.Entry e : ClientSkills.skills()) {
            if (e.level() >= e.maxLevel()) {
                Button max = Button.builder(Component.translatable("gui.fishinggui.skills.max"), b -> { })
                        .bounds(btnX, y + 12, BTN_W, 20).build();
                max.active = false;
                addRenderableWidget(max);
            } else {
                int cost = e.cost().get(e.level());
                Button buy = Button.builder(Component.translatable("gui.fishinggui.skills.buy", cost),
                        b -> ModNetwork.CHANNEL.sendToServer(new SkillBuyPacket(e.id())))
                        .bounds(btnX, y + 12, BTN_W, 20).build();
                buy.active = ClientSkills.points() >= cost;
                addRenderableWidget(buy);
            }
            if (hasDrop) {
                Button reset = Button.builder(
                        Component.translatable("gui.fishinggui.skills.reset").withStyle(ChatFormatting.RED),
                        b -> confirmReset(e.id())).bounds(resetX, y + 12, RESET_W, 20).build();
                reset.active = e.level() > 0;
                addRenderableWidget(reset);
            }
            y += ROW_H;
        }
    }

    private void confirmReset(String skillId) {
        Minecraft mc = Minecraft.getInstance();
        Component name = Component.translatable("skill.fishinggui." + skillId);
        mc.setScreen(new ConfirmScreen(yes -> {
            if (yes) {
                ModNetwork.CHANNEL.sendToServer(new SkillResetPacket(skillId));
            }
            mc.setScreen(this);
        }, Component.translatable("gui.fishinggui.skills.reset"),
                Component.translatable("gui.fishinggui.skills.reset.confirm", name)));
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
            Component name = Component.translatable("skill.fishinggui." + e.id());
            g.drawString(this.font, name, left, y + 2, 0xFFFFFF);
            g.drawString(this.font, "Lv." + e.level() + "/" + e.maxLevel(), left + this.font.width(name) + 8, y + 2,
                    0x55FF55);
            int textW = ROW_W - BTN_W - RESET_W - GAP * 2 - 6;
            int ly = y + 14;
            for (var line : this.font.split(Component.translatable("skill.fishinggui." + e.id() + ".desc"), textW)) {
                g.drawString(this.font, line, left, ly, 0xAAAAAA);
                ly += 10;
            }
            g.fill(left, y + ROW_H - 4, left + ROW_W, y + ROW_H - 3, 0x44FFFFFF);
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
