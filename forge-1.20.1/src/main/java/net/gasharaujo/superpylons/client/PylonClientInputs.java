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

    @SubscribeEvent
    public static void commandBlockTooltip(net.minecraftforge.event.entity.player.ItemTooltipEvent event) {
        if (!event.getItemStack().is(net.minecraft.world.item.Items.COMMAND_BLOCK)) return;
        event.getToolTip().add(net.minecraft.network.chat.Component.translatable("tooltip.gash_super_pylons.category.utility")
            .withStyle(net.minecraft.ChatFormatting.BLUE));
        event.getToolTip().add(net.minecraft.network.chat.Component.translatable("tooltip.gash_super_pylons.creative_storage")
            .withStyle(net.minecraft.ChatFormatting.BLUE));
    }
}
