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
            PylonUpgrade auxiliary = entity.auxiliaryMode();
            if (auxiliary == PylonUpgrade.ENCHANTING) {
                workshop.renderBook(ticks, partialTick, pose, buffers, light, overlay);
                return;
            }
            if (auxiliary == PylonUpgrade.ANVIL) workshop.renderOrbs(ticks, pose, buffers, light, overlay);
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
        int shelfSlot = PylonUpgradeStamps.shelfSlot(upgrades, toolSlot);
        int total = upgrades.size() + (toolSlot >= 0 ? 1 : 0) + shelves.size();
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        var sprites = PylonUpgradeStamps.sprites(upgrades, id -> atlas.apply(ResourceLocation.fromNamespaceAndPath(SuperPylonsMod.MOD_ID, "item/" + id)));
        var buffer = buffers.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));
        var matrix = pose.last();
        PylonUpgradeStamps.draw(sprites, toolSlot, shelfSlot, shelves.size(), (x, y, z, normal, u, v) -> buffer.addVertex(matrix, x, y, z)
            .setColor(-1).setUv(u, v).setOverlay(overlay).setLight(light)
            .setNormal(matrix, normal.getStepX(), normal.getStepY(), normal.getStepZ()));
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
