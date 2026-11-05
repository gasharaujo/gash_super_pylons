package net.gasharaujo.superpylons.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.gasharaujo.superpylons.block.PylonUpgrade;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.gasharaujo.superpylons.block.PylonWire;
import net.gasharaujo.superpylons.block.SuperPylonBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class SuperPylonRenderer implements BlockEntityRenderer<SuperPylonBlockEntity, SuperPylonRenderer.State> {
    private final PylonCoreRenderer core = new PylonCoreRenderer();
    private final PylonWorkshopRenderer workshop;

    public SuperPylonRenderer(BlockEntityRendererProvider.Context context) {
        workshop = new PylonWorkshopRenderer(context);
    }

    public static final class State extends BlockEntityRenderState {
        public float ticks;
        @Nullable PylonUpgrade auxiliary;
        @Nullable PylonFluidCore fluid;
        PylonDecorationRenderer.@Nullable Frame frame;
        List<PylonWire> wires = List.of();
        List<TextureAtlasSprite> stamps = List.of();
        int toolSlot = -1;
        int shelfSlot = -1;
        List<ItemStackRenderState> shelves = List.of();
        public final ItemStackRenderState storedItem = new ItemStackRenderState();
        public final ItemStackRenderState breakTool = new ItemStackRenderState();
        final ItemStackRenderState superStar = new ItemStackRenderState();
    }

    @Override
    public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(SuperPylonBlockEntity entity, State state, float partialTick, Vec3 camera,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, camera, breaking);
        state.ticks = entity.getLevel() == null ? 0 :
            PylonAnimation.ticks(entity.getLevel().getGameTime(), partialTick);
        state.fluid = PylonCoreRenderer.extractFluid(entity, state.lightCoords);
        state.auxiliary = entity.auxiliaryMode();
        state.frame = PylonDecorationRenderer.frame(entity);
        state.wires = entity.outgoingWires();
        var upgrades = entity.getInstalledUpgrades();
        if (upgrades.isEmpty()) {
            state.stamps = List.of();
        } else {
            var atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.ITEMS);
            state.stamps = PylonUpgradeStamps.sprites(upgrades,
                id -> atlas.getSprite(Identifier.parse(id)));
        }
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(state.storedItem,
            entity.getStoredItem(), ItemDisplayContext.FIXED, entity.getLevel(), null, (int) entity.getBlockPos().asLong());
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(state.breakTool,
            entity.getBreakTool(), ItemDisplayContext.GUI, entity.getLevel(), null, (int) entity.getBlockPos().asLong());
        state.toolSlot = PylonUpgradeStamps.toolSlot(!state.breakTool.isEmpty(), entity.hasSuperUpgrade());
        state.shelfSlot = PylonUpgradeStamps.shelfSlot(upgrades, state.toolSlot);
        state.shelves = entity.getShelfItems().stream().map(item -> {
            var render = new ItemStackRenderState();
            Minecraft.getInstance().getItemModelResolver().updateForTopItem(render, item,
                ItemDisplayContext.GUI, entity.getLevel(), null, (int) entity.getBlockPos().asLong());
            return render;
        }).toList();
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(state.superStar,
            entity.hasSuperUpgrade() ? new ItemStack(Items.NETHER_STAR) : ItemStack.EMPTY,
            ItemDisplayContext.FIXED, entity.getLevel(), null, 0);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        PylonDecorationRenderer.submit(state.frame, state.wires, state.blockPos, state.ticks, pose, collector, state.lightCoords);
        if (!state.stamps.isEmpty()) {
            var stamps = state.stamps;
            int light = state.lightCoords;
            collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TextureAtlas.LOCATION_ITEMS), (matrix, buffer) ->
                PylonUpgradeStamps.draw(stamps, state.toolSlot, state.shelfSlot, state.shelves.size(), (x, y, z, normal, u, v) -> buffer.addVertex(matrix, x, y, z)
                    .setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                    .setNormal(matrix, normal.getStepX(), normal.getStepY(), normal.getStepZ())));
        }
        if (!state.breakTool.isEmpty()) {
            for (Direction face : Direction.Plane.HORIZONTAL) {
                pose.pushPose();
                PylonUpgradeStamps.placeItem(pose, face, state.stamps.size() + state.shelves.size() + 1, state.toolSlot);
                state.breakTool.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                pose.popPose();
            }
        }
        for (int i = 0; i < state.shelves.size(); i++) {
            for (Direction face : Direction.Plane.HORIZONTAL) {
                pose.pushPose();
                PylonUpgradeStamps.placeItem(pose, face,
                    state.stamps.size() + state.shelves.size() + (state.toolSlot >= 0 ? 1 : 0), state.shelfSlot + i);
                state.shelves.get(i).submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                pose.popPose();
            }
        }
        if (!state.superStar.isEmpty()) {
            for (int star = 0; star < PylonAnimation.SUPER_STAR_COUNT; star++) {
                pose.pushPose();
                PylonAnimation.placeStar(pose, state.ticks, star, camera.orientation);
                state.superStar.submit(pose, collector, 0xF000F0, OverlayTexture.NO_OVERLAY, 0);
                pose.popPose();
            }
        }
        if (state.auxiliary == PylonUpgrade.ANVIL) workshop.submitOrbs(state, pose, collector, camera);
        if (state.auxiliary == PylonUpgrade.ENCHANTING) {
            workshop.submitBook(state, pose, collector, camera);
        } else if (state.fluid != null) {
            core.submitFluid(state.fluid, pose, collector, state.ticks, OverlayTexture.NO_OVERLAY);
        } else if (state.storedItem.isEmpty()) {
            core.submit(pose, collector, state.ticks, state.lightCoords, OverlayTexture.NO_OVERLAY,
                0, state.breakProgress);
        } else {
            pose.pushPose();
            PylonAnimation.applyOrbit(pose, state.ticks);
            pose.scale(0.5F, 0.5F, 0.5F);
            state.storedItem.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }
}
