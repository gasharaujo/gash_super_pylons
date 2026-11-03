package net.gasharaujo.superpylons.client;

import net.gasharaujo.superpylons.SuperPylonsMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.EntityRenderersEvent;

@Mod.EventBusSubscriber(modid = SuperPylonsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(SuperPylonsMod.SUPER_PYLON_ENTITY.get(), context -> new SuperPylonRenderer());
    }

}
