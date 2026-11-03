package net.gasharaujo.superpylons.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.gasharaujo.superpylons.block.SuperPylonBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class SuperPylonRenderer implements BlockEntityRenderer<SuperPylonBlockEntity, SuperPylonRenderer.State> {
    private final PylonCoreRenderer core = new PylonCoreRenderer();

    public static final class State extends BlockEntityRenderState {
        public float ticks;
    }

    @Override
    public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(SuperPylonBlockEntity entity, State state, float partialTick, Vec3 camera,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, camera, breaking);
        state.ticks = entity.getLevel() == null ? 0 :
            PylonAnimation.ticks(entity.getLevel().getGameTime(), partialTick);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        core.submit(pose, collector, state.ticks, state.lightCoords, OverlayTexture.NO_OVERLAY,
            0, state.breakProgress);
    }
}
