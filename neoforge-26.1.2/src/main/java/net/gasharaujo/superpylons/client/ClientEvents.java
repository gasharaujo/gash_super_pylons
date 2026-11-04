package net.gasharaujo.superpylons.client;

import net.gasharaujo.superpylons.SuperPylonsMod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;

@EventBusSubscriber(modid = SuperPylonsMod.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(SuperPylonsMod.FILTER_MENU.get(), PylonFilterScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(SuperPylonsMod.SUPER_PYLON_ENTITY.get(), context -> new SuperPylonRenderer());
    }

    @SubscribeEvent
    public static void registerSpecialRenderer(RegisterSpecialModelRendererEvent event) {
        event.register(Identifier.fromNamespaceAndPath(SuperPylonsMod.MOD_ID, "super_pylon"),
            SuperPylonItemRenderer.Unbaked.MAP_CODEC);
    }

}
