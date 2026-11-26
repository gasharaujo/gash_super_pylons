package net.gasharaujo.superpylons.client;

import net.gasharaujo.superpylons.SuperPylonsMod;
import net.gasharaujo.superpylons.block.PylonXpFluidRegistration;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSources;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
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
    public static void registerPonder(FMLClientSetupEvent event) {
        if (ModList.get().isLoaded("ponder")) event.enqueueWork((Runnable) ClientEvents::registerPonder);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(SuperPylonsMod.FILTER_MENU.get(), PylonFilterScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(SuperPylonsMod.SUPER_PYLON_ENTITY.get(), SuperPylonRenderer::new);
    }

    @SubscribeEvent
    public static void registerSpecialRenderer(RegisterSpecialModelRendererEvent event) {
        event.register(Identifier.fromNamespaceAndPath(SuperPylonsMod.MOD_ID, "super_pylon"),
            SuperPylonItemRenderer.Unbaked.MAP_CODEC);
    }

    @SubscribeEvent
    public static void registerFluidModels(RegisterFluidModelsEvent event) {
        Material still = new Material(Identifier.withDefaultNamespace("block/water_still"));
        Material flowing = new Material(Identifier.withDefaultNamespace("block/water_flow"));
        event.register(new FluidModel.Unbaked(still, flowing, null, FluidTintSources.constant(0xFF81CF2F)),
            PylonXpFluidRegistration.FALLBACK);
    }

    private static void registerPonder() {
        try {
            Class.forName("net.gasharaujo.superpylons.client.PylonPonder")
                .getDeclaredMethod("register").invoke(null);
        } catch (ClassNotFoundException ignored) {
            // The 26.3 build includes this integration when a Ponder API is supplied.
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not register Ponder scenes", e);
        }
    }
}
