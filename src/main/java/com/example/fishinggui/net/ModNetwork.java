package com.example.fishinggui.net;

import com.example.fishinggui.FishingGuiMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(FishingGuiMod.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private ModNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(FightStartPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FightStartPacket::encode).decoder(FightStartPacket::decode)
                .consumerMainThread(FightStartPacket::handle).add();
        CHANNEL.messageBuilder(FightStatePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FightStatePacket::encode).decoder(FightStatePacket::decode)
                .consumerMainThread(FightStatePacket::handle).add();
        CHANNEL.messageBuilder(ReelInputPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ReelInputPacket::encode).decoder(ReelInputPacket::decode)
                .consumerMainThread(ReelInputPacket::handle).add();
        CHANNEL.messageBuilder(CatchResultPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CatchResultPacket::encode).decoder(CatchResultPacket::decode)
                .consumerMainThread(CatchResultPacket::handle).add();
        CHANNEL.messageBuilder(CatchChoicePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CatchChoicePacket::encode).decoder(CatchChoicePacket::decode)
                .consumerMainThread(CatchChoicePacket::handle).add();
        CHANNEL.messageBuilder(FishingProgressPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FishingProgressPacket::encode).decoder(FishingProgressPacket::decode)
                .consumerMainThread(FishingProgressPacket::handle).add();
        CHANNEL.messageBuilder(SkillSyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SkillSyncPacket::encode).decoder(SkillSyncPacket::decode)
                .consumerMainThread(SkillSyncPacket::handle).add();
        CHANNEL.messageBuilder(SkillBuyPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SkillBuyPacket::encode).decoder(SkillBuyPacket::decode)
                .consumerMainThread(SkillBuyPacket::handle).add();
        CHANNEL.messageBuilder(SkillResetPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SkillResetPacket::encode).decoder(SkillResetPacket::decode)
                .consumerMainThread(SkillResetPacket::handle).add();
    }
}
