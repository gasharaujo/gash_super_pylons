package net.gasharaujo.superpylons.client;

import net.gasharaujo.superpylons.SuperPylonsMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

@EventBusSubscriber(modid = SuperPylonsMod.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(SuperPylonsMod.SUPER_PYLON_ENTITY.get(), context -> new SuperPylonRenderer());
    }

    @SubscribeEvent
    public static void registerItemExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private final SuperPylonItemRenderer renderer = new SuperPylonItemRenderer();
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() { return renderer; }
        }, SuperPylonsMod.SUPER_PYLON_ITEM.get());
    }

}
