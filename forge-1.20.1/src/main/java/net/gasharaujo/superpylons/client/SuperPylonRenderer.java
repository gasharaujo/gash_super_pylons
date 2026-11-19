package net.gasharaujo.superpylons.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.gasharaujo.superpylons.block.PylonUpgrade;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.gasharaujo.superpylons.block.SuperPylonBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;

public final class SuperPylonRenderer implements BlockEntityRenderer<SuperPylonBlockEntity> {
    private final PylonCoreRenderer core = new PylonCoreRenderer();
    private final PylonWorkshopRenderer workshop;

    public SuperPylonRenderer(BlockEntityRendererProvider.Context context) {
        workshop = new PylonWorkshopRenderer(context);
    }

    @Override
    public void render(SuperPylonBlockEntity entity, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        if (entity.getLevel() != null) {
            float ticks = PylonAnimation.ticks(entity.getLevel().getGameTime(), partialTick);
            PylonDecorationRenderer.render(entity, ticks, pose, buffers, light, overlay);
            renderStamps(entity, pose, buffers, light, overlay);
            if (entity.hasSuperUpgrade()) renderStars(entity, ticks, pose, buffers, overlay);
            if (entity.isHub()) {
                renderHub(entity, ticks, pose, buffers, light, overlay);
                return;
            }
            PylonUpgrade auxiliary = entity.auxiliaryMode();
            if (auxiliary == PylonUpgrade.ENCHANTING) {
                workshop.renderBook(ticks, partialTick, pose, buffers, light, overlay);
                return;
            }
            if (auxiliary == PylonUpgrade.ANVIL || auxiliary == PylonUpgrade.GRINDSTONE) {
                workshop.renderOrbs(auxiliary == PylonUpgrade.GRINDSTONE ? -ticks : ticks, pose, buffers, light, overlay);
            }
            if (core.renderFluid(entity, pose, buffers, ticks, light, overlay)) return;
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

    @Override
    public boolean shouldRenderOffScreen(SuperPylonBlockEntity entity) {
        return !entity.outgoingWires().isEmpty();
    }

    private static void renderHub(SuperPylonBlockEntity entity, float ticks, PoseStack pose,
                                  MultiBufferSource buffers, int light, int overlay) {
        var renderer = Minecraft.getInstance().getItemRenderer();
        var center = new ItemStack(Items.ENDER_CHEST);
        var chest = new ItemStack(Items.CHEST);
        pose.pushPose();
        PylonAnimation.placeHubCenter(pose, ticks);
        renderer.renderStatic(center, ItemDisplayContext.FIXED, light, overlay, pose, buffers, entity.getLevel(), 0);
        pose.popPose();
        for (var ring : PylonAnimation.HubRing.values()) {
            for (int index = 0; index < PylonAnimation.HUB_CHESTS_PER_RING; index++) {
                pose.pushPose();
                PylonAnimation.placeHubChest(pose, ticks, ring, index);
                renderer.renderStatic(chest, ItemDisplayContext.FIXED, light, overlay, pose, buffers, entity.getLevel(), 0);
                pose.popPose();
            }
        }
    }

    private static void renderStars(SuperPylonBlockEntity entity, float ticks, PoseStack pose,
                                    MultiBufferSource buffers, int overlay) {
        var minecraft = Minecraft.getInstance();
        var camera = minecraft.getEntityRenderDispatcher().cameraOrientation();
        var netherStar = new ItemStack(Items.NETHER_STAR);
        for (int star = 0; star < PylonAnimation.SUPER_STAR_COUNT; star++) {
            pose.pushPose();
            PylonAnimation.placeStar(pose, ticks, star, camera);
            minecraft.getItemRenderer().renderStatic(netherStar, ItemDisplayContext.FIXED,
                0xF000F0, overlay, pose, buffers, entity.getLevel(), star);
            pose.popPose();
        }
    }

    private static void renderStamps(SuperPylonBlockEntity entity, PoseStack pose, MultiBufferSource buffers,
                                     int light, int overlay) {
        var upgrades = entity.getInstalledUpgrades();
        if (upgrades.isEmpty()) return;
        ItemStack tool = entity.getBreakTool();
        int toolSlot = PylonUpgradeStamps.toolSlot(!tool.isEmpty(), entity.hasSuperUpgrade());
        var shelves = entity.getShelfItems();
        int shelfSlot = PylonUpgradeStamps.shelfSlot(upgrades, toolSlot, !entity.getFurnaceAttachment().isEmpty());
        var minecraft = Minecraft.getInstance();
        var atlas = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        var stamps = PylonUpgradeStamps.stamps(upgrades, entity.getFurnaceAttachment(),
            id -> atlas.apply(new ResourceLocation(id)), upgrade -> new ItemStack(SuperPylonsMod.UPGRADES.get(upgrade).get()));
        int total = stamps.size() + (toolSlot >= 0 ? 1 : 0) + shelves.size();
        if (stamps.stream().anyMatch(stamp -> stamp.sprite() != null)) {
            var buffer = buffers.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));
            var matrix = pose.last();
            PylonUpgradeStamps.draw(stamps, toolSlot, shelfSlot, shelves.size(),
                (x, y, z, normal, u, v) -> buffer.vertex(matrix.pose(), x, y, z)
                    .color(255, 255, 255, 255).uv(u, v).overlayCoords(overlay).uv2(light)
                    .normal(matrix.normal(), normal.getStepX(), normal.getStepY(), normal.getStepZ()).endVertex());
        }
        for (int i = 0; i < stamps.size(); i++) {
            ItemStack model = stamps.get(i).model();
            if (model == null) continue;
            int slot = PylonUpgradeStamps.stampSlot(i, toolSlot, shelfSlot, shelves.size());
            for (Direction face : Direction.Plane.HORIZONTAL) {
                pose.pushPose();
                PylonUpgradeStamps.placeItem(pose, face, total, slot);
                minecraft.getItemRenderer().renderStatic(model, ItemDisplayContext.NONE,
                    light, overlay, pose, buffers, entity.getLevel(), (int) entity.getBlockPos().asLong());
                pose.popPose();
            }
        }
        if (!tool.isEmpty()) {
            for (Direction face : Direction.Plane.HORIZONTAL) {
                pose.pushPose();
                PylonUpgradeStamps.placeItem(pose, face, total, toolSlot);
                Minecraft.getInstance().getItemRenderer().renderStatic(tool, ItemDisplayContext.GUI,
                    light, overlay, pose, buffers, entity.getLevel(), (int) entity.getBlockPos().asLong());
                pose.popPose();
            }
        }
        for (int i = 0; i < shelves.size(); i++) {
            for (Direction face : Direction.Plane.HORIZONTAL) {
                pose.pushPose();
                PylonUpgradeStamps.placeItem(pose, face, total, shelfSlot + i);
                Minecraft.getInstance().getItemRenderer().renderStatic(shelves.get(i), ItemDisplayContext.GUI,
                    light, overlay, pose, buffers, entity.getLevel(), (int) entity.getBlockPos().asLong());
                pose.popPose();
            }
        }
    }
}
