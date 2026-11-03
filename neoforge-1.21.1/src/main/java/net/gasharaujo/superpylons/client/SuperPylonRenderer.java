package net.gasharaujo.superpylons.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.gasharaujo.superpylons.block.SuperPylonBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;

public final class SuperPylonRenderer implements BlockEntityRenderer<SuperPylonBlockEntity> {
    private final PylonCoreRenderer core = new PylonCoreRenderer();

    @Override
    public void render(SuperPylonBlockEntity entity, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        if (entity.getLevel() != null) {
            core.render(pose, buffers, PylonAnimation.ticks(entity.getLevel().getGameTime(), partialTick),
                light, overlay);
        }
    }
}
