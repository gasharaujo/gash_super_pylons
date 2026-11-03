package net.gasharaujo.superpylons.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class SuperPylonItemRenderer extends BlockEntityWithoutLevelRenderer {
    private final PylonCoreRenderer core = new PylonCoreRenderer();

    public SuperPylonItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
            SuperPylonsMod.SUPER_PYLON.get().defaultBlockState(), pose, buffers, light, overlay);
        core.render(pose, buffers, PylonAnimation.itemTicks(Util.getMillis()), light, overlay);
    }
}
