package net.gasharaujo.superpylons.client;

import net.gasharaujo.superpylons.PylonControls;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.gasharaujo.superpylons.block.PylonControlRequest;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SuperPylonsMod.MOD_ID, value = Dist.CLIENT)
public final class PylonClientInputs {
    private PylonClientInputs() {}

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        var minecraft = Minecraft.getInstance();
        if (event.getScrollDelta() == 0 || minecraft.screen != null || minecraft.player == null ||
            !minecraft.player.isShiftKeyDown() || !minecraft.player.getMainHandItem().is(SuperPylonsMod.WRENCH.get())) return;
        PylonControls.send(PylonControlRequest.wrench());
        event.setCanceled(true);
    }
}
