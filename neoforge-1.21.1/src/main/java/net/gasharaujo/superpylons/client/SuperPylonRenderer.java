package net.gasharaujo.superpylons.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.gasharaujo.superpylons.block.SuperPylonBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;

public final class SuperPylonRenderer implements BlockEntityRenderer<SuperPylonBlockEntity> {
    private final PylonCoreRenderer core = new PylonCoreRenderer();

    @Override
    public void render(SuperPylonBlockEntity entity, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        if (entity.getLevel() != null) {
            float ticks = PylonAnimation.ticks(entity.getLevel().getGameTime(), partialTick);
            ItemStack stored = entity.getStoredItem();
            if (stored.isEmpty()) {
                core.render(pose, buffers, ticks, light, overlay);
            } else {
                pose.pushPose();
                PylonAnimation.applyOrbit(pose, ticks);
                pose.scale(0.5F, 0.5F, 0.5F);
                Minecraft.getInstance().getItemRenderer().renderStatic(stored, ItemDisplayContext.FIXED,
                    light, overlay, pose, buffers, entity.getLevel(), (int) entity.getBlockPos().asLong());
                pose.popPose();
            }
        }
    }
}
