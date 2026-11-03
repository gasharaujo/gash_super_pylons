package net.gasharaujo.superpylons.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public final class PylonCoreRenderer {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
        SuperPylonsMod.MOD_ID, "textures/block/core.png");
    private final ModelPart core = PylonCoreModel.create();

    public void submit(PoseStack pose, SubmitNodeCollector collector, float ticks, int light, int overlay,
                       int outline, ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        pose.pushPose();
        PylonAnimation.apply(pose, ticks);
        collector.submitModelPart(core, pose, RenderTypes.entitySolid(TEXTURE), light, overlay,
            null, false, false, -1, breaking, outline);
        pose.popPose();
    }
}
