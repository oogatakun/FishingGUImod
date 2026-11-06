package com.example.fishinggui.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Client-only entry points called from common code through DistExecutor. */
public final class ClientHooks {
    private ClientHooks() {
    }

    public static void openTakoyakiScreen(BlockPos pos) {
        Minecraft.getInstance().setScreen(new TakoyakiScreen(pos));
    }
}
