package net.gasharaujo.superpylons.client;

import net.gasharaujo.superpylons.PylonControls;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.gasharaujo.superpylons.block.PylonControlRequest;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(modid = SuperPylonsMod.MOD_ID, value = Dist.CLIENT)
public final class PylonClientInputs {
    private PylonClientInputs() {}

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        var minecraft = Minecraft.getInstance();
        if (event.getScrollDeltaY() == 0 || minecraft.screen != null || minecraft.player == null ||
            !minecraft.player.isShiftKeyDown() || !minecraft.player.getMainHandItem().is(SuperPylonsMod.WRENCH.get())) return;
        PylonControls.send(PylonControlRequest.wrench());
        event.setCanceled(true);
    }
}
