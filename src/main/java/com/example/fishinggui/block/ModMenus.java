package com.example.fishinggui.block;

import com.example.fishinggui.FishingGuiMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, FishingGuiMod.MOD_ID);

    public static final RegistryObject<MenuType<TackleBenchMenu>> TACKLE_BENCH = MENUS.register("tackle_bench",
            () -> IForgeMenuType.create((id, inv, buf) -> new TackleBenchMenu(id, inv, ContainerLevelAccess.NULL)));

    private ModMenus() {
    }

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
