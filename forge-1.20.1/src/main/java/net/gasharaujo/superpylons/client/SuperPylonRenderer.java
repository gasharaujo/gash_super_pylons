package net.gasharaujo.superpylons.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.gasharaujo.superpylons.block.SuperPylonBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;

public final class SuperPylonRenderer implements BlockEntityRenderer<SuperPylonBlockEntity> {
    private final PylonCoreRenderer core = new PylonCoreRenderer();

    @Override
    public void render(SuperPylonBlockEntity entity, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        if (entity.getLevel() != null) {
            float ticks = PylonAnimation.ticks(entity.getLevel().getGameTime(), partialTick);
            PylonDecorationRenderer.render(entity, ticks, pose, buffers, light, overlay);
            renderStamps(entity, pose, buffers, light, overlay);
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

    private static void renderStamps(SuperPylonBlockEntity entity, PoseStack pose, MultiBufferSource buffers,
                                     int light, int overlay) {
        var upgrades = entity.getInstalledUpgrades();
        if (upgrades.isEmpty()) return;
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        var sprites = PylonUpgradeStamps.sprites(upgrades, id -> atlas.apply(new ResourceLocation(SuperPylonsMod.MOD_ID, "item/" + id)));
        var buffer = buffers.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));
        var matrix = pose.last();
        PylonUpgradeStamps.draw(sprites, (x, y, z, normal, u, v) -> buffer.vertex(matrix.pose(), x, y, z)
            .color(255, 255, 255, 255).uv(u, v).overlayCoords(overlay).uv2(light)
            .normal(matrix.normal(), normal.getStepX(), normal.getStepY(), normal.getStepZ()).endVertex());
    }
}
