package net.gasharaujo.superpylons.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;

final class PylonWorkshopRenderer {
    private static final float[] X = {-0.06F, 0.06F, 0.06F, -0.06F};
    private static final float[] Y = {-0.06F, -0.06F, 0.06F, 0.06F};
    private static final ResourceLocation ORB = ResourceLocation.withDefaultNamespace("textures/entity/experience_orb.png");
    private final EnchantTableRenderer book;
    private final EnchantingTableBlockEntity bookState = new EnchantingTableBlockEntity(BlockPos.ZERO,
        Blocks.ENCHANTING_TABLE.defaultBlockState());

    PylonWorkshopRenderer(BlockEntityRendererProvider.Context context) {
        book = new EnchantTableRenderer(context);
    }

    void renderBook(float ticks, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        bookState.time = (int) ticks;
        bookState.open = bookState.oOpen = 1;
        bookState.flip = bookState.oFlip = ticks / 20;
        bookState.rot = bookState.oRot = ticks * (float) (Math.PI / 80);
        pose.pushPose();
        pose.translate(0, -0.35, 0);
        book.render(bookState, partialTick, pose, buffers, light, overlay);
        pose.popPose();
    }

    void renderOrbs(float ticks, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var camera = Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation();
        var buffer = buffers.getBuffer(RenderType.itemEntityTranslucentCull(ORB));
        for (int orb = 0; orb < 4; orb++) {
            var point = PylonAnimation.xpOrbit(ticks, orb);
            pose.pushPose();
            pose.translate(point.x, point.y, point.z);
            pose.mulPose(camera);
            var matrix = pose.last();
            for (int i = 0; i < 4; i++) {
                buffer.addVertex(matrix, X[i], Y[i], 0).setColor(135, 255, 48, 210)
                    .setUv(X[i] > 0 ? 0.25F : 0, Y[i] > 0 ? 0 : 0.25F)
                    .setLight(0xF000F0).setOverlay(overlay).setNormal(matrix, 0, 0, 1);
            }
            pose.popPose();
        }
    }
}
