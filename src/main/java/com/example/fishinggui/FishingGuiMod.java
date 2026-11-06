package com.example.fishinggui;

import com.example.fishinggui.item.ModItems;
import com.example.fishinggui.net.ModNetwork;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(FishingGuiMod.MOD_ID)
public class FishingGuiMod {
    public static final String MOD_ID = "fishinggui";

    public FishingGuiMod() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::commonSetup);
        ModItems.register(modBus);
        com.example.fishinggui.block.ModBlocks.register(modBus);
        com.example.fishinggui.block.ModMenus.register(modBus);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}
