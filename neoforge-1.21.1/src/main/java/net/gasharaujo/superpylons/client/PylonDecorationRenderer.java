package net.gasharaujo.superpylons.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.gasharaujo.superpylons.block.PylonFrame;
import net.gasharaujo.superpylons.block.SuperPylonBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

final class PylonDecorationRenderer {
    private static final ResourceLocation WHITE = ResourceLocation.fromNamespaceAndPath(SuperPylonsMod.MOD_ID, "textures/block/core.png");

    private PylonDecorationRenderer() {}

    static void render(SuperPylonBlockEntity entity, float ticks, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        if (entity.getBlockState().getValue(PylonFrame.PAINTED)) {
            var renderer = Minecraft.getInstance().getBlockRenderer();
            var state = entity.framePaint().defaultBlockState();
            var sprite = renderer.getBlockModel(state).getParticleIcon();
            if (sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
                state = Blocks.IRON_BLOCK.defaultBlockState();
                sprite = renderer.getBlockModel(state).getParticleIcon();
            }
            boolean translucent = ItemBlockRenderTypes.getChunkRenderType(state) == RenderType.translucent();
            var type = translucent ? RenderType.entityTranslucent(sprite.atlasLocation()) : RenderType.entityCutout(sprite.atlasLocation());
            PylonDecorationMesh.frame(sprite, sink(pose.last(), buffers.getBuffer(type), light, overlay));
        }
        var wires = entity.outgoingWires();
        if (!wires.isEmpty()) {
            PylonDecorationMesh.wires(entity.getBlockPos(), wires, ticks,
                sink(pose.last(), buffers.getBuffer(RenderType.entityCutout(WHITE)), light, overlay));
        }
    }

    private static PylonDecorationMesh.VertexSink sink(PoseStack.Pose matrix, VertexConsumer buffer, int light, int overlay) {
        return (x, y, z, u, v, color, nx, ny, nz) -> buffer.addVertex(matrix, x, y, z)
            .setColor(color).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(matrix, nx, ny, nz);
    }
}
