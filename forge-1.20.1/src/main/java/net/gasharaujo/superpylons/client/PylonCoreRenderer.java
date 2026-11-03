package net.gasharaujo.superpylons.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class PylonCoreRenderer {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
        SuperPylonsMod.MOD_ID, "textures/block/core.png");
    private final ModelPart core = PylonCoreModel.create();

    public void render(PoseStack pose, MultiBufferSource buffers, float ticks, int light, int overlay) {
        pose.pushPose();
        PylonAnimation.apply(pose, ticks);
        core.render(pose, buffers.getBuffer(RenderType.entitySolid(TEXTURE)), light, overlay);
        pose.popPose();
    }
}
