package net.gasharaujo.superpylons.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.client.renderer.blockentity.state.EnchantTableRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

final class PylonWorkshopRenderer {
    private static final float[] X = {-0.06F, 0.06F, 0.06F, -0.06F};
    private static final float[] Y = {-0.06F, -0.06F, 0.06F, 0.06F};
    private static final Identifier ORB = Identifier.withDefaultNamespace("textures/entity/experience/experience_orb.png");
    private final EnchantTableRenderer book;

    PylonWorkshopRenderer(BlockEntityRendererProvider.Context context) {
        book = new EnchantTableRenderer(context);
    }

    void submitBook(SuperPylonRenderer.State pylon, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        var state = new EnchantTableRenderState();
        state.lightCoords = pylon.lightCoords;
        state.time = pylon.ticks;
        state.open = 1;
        state.flip = pylon.ticks / 20;
        state.yRot = pylon.ticks * (float) (Math.PI / 80);
        pose.pushPose();
        pose.translate(0, -0.35, 0);
        book.submit(state, pose, collector, camera);
        pose.popPose();
    }

    void submitOrbs(SuperPylonRenderer.State pylon, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        for (int orb = 0; orb < 4; orb++) {
            var point = PylonAnimation.xpOrbit(pylon.ticks, orb);
            pose.pushPose();
            pose.translate(point.x, point.y, point.z);
            pose.mulPose(camera.orientation);
            collector.submitCustomGeometry(pose, RenderTypes.entityTranslucentCullItemTarget(ORB), (matrix, buffer) -> {
                for (int i = 0; i < 4; i++) {
                    buffer.addVertex(matrix, X[i], Y[i], 0).setColor(135, 255, 48, 210)
                        .setUv(X[i] > 0 ? 0.25F : 0, Y[i] > 0 ? 0 : 0.25F)
                        .setLight(0xF000F0).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(matrix, 0, 0, 1);
                }
            });
            pose.popPose();
        }
    }
}
