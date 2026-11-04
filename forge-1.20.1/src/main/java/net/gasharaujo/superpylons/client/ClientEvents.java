package net.gasharaujo.superpylons.client;

import net.gasharaujo.superpylons.SuperPylonsMod;
import net.minecraftforge.fml.ModList;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.EntityRenderersEvent;

@Mod.EventBusSubscriber(modid = SuperPylonsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void registerPonder(FMLClientSetupEvent event) {
        if (ModList.get().isLoaded("ponder")) event.enqueueWork(PylonPonder::register);
    }

    @SubscribeEvent
    public static void registerScreens(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(SuperPylonsMod.FILTER_MENU.get(), PylonFilterScreen::new));
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(SuperPylonsMod.SUPER_PYLON_ENTITY.get(), SuperPylonRenderer::new);
    }

}
