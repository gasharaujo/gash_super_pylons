package net.gasharaujo.superpylons.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.gasharaujo.superpylons.block.SuperPylonBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.material.Fluids;
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

    static @Nullable PylonFluidCore extractFluid(SuperPylonBlockEntity entity, int light) {
        var handler = entity.getFluidHandler();
        if (handler == null) return null;
        var resource = handler.getResource(0);
        if (resource.isEmpty()) return null;

        var fluid = resource.getFluid();
        var state = fluid.defaultFluidState();
        var model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(state);
        var sprite = model.stillMaterial().sprite();
        if (sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) return null;

        var stack = resource.toStack(handler.getAmountAsInt(0));
        var tintSource = model.fluidTintSource();
        int tint = tintSource == null ? -1 : tintSource.colorAsStack(stack);
        if (tintSource != null && (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER)
            && entity.getLevel() instanceof ClientLevel level) {
            tint = tintSource.colorInWorld(state, state.createLegacyBlock(), level, entity.getBlockPos()) | 0xFF000000;
        }
        int fluidLight = LightCoordsUtil.pack(
            Math.max(LightCoordsUtil.block(light), fluid.getFluidType().getLightLevel(stack)), LightCoordsUtil.sky(light));
        boolean translucent = model.layer() == ChunkSectionLayer.TRANSLUCENT || (tint >>> 24) < 255;
        return new PylonFluidCore(sprite, tint, fluidLight, translucent);
    }

    void submitFluid(PylonFluidCore fluid, PoseStack pose, SubmitNodeCollector collector, float ticks, int overlay) {
        var atlas = fluid.sprite().atlasLocation();
        RenderType type = fluid.translucent() ? RenderTypes.entityTranslucent(atlas) : RenderTypes.entityCutout(atlas);
        pose.pushPose();
        PylonAnimation.apply(pose, ticks);
        collector.submitCustomGeometry(pose, type, (matrix, buffer) ->
            fluid.draw((corner, normal, u, v) -> buffer.addVertex(matrix, corner.x(), corner.y(), corner.z())
                .setColor(fluid.tint()).setUv(u, v).setOverlay(overlay).setLight(fluid.light())
                .setNormal(matrix, normal.getStepX(), normal.getStepY(), normal.getStepZ())));
        pose.popPose();
    }
}
