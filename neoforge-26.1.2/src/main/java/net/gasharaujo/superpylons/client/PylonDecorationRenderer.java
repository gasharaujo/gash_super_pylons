package net.gasharaujo.superpylons.client;

import com.mojang.blaze3d.platform.Transparency;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.gasharaujo.superpylons.block.PylonFrame;
import net.gasharaujo.superpylons.block.PylonWire;
import net.gasharaujo.superpylons.block.SuperPylonBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;

final class PylonDecorationRenderer {
    private static final Identifier WHITE = Identifier.fromNamespaceAndPath(SuperPylonsMod.MOD_ID, "textures/block/core.png");

    private PylonDecorationRenderer() {}

    record Frame(TextureAtlasSprite sprite, boolean translucent) {}

    static Frame frame(SuperPylonBlockEntity entity) {
        if (!entity.getBlockState().getValue(PylonFrame.PAINTED)) return null;
        var models = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        var material = models.getParticleMaterial(entity.framePaint().defaultBlockState());
        if (material.sprite().contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
            material = models.getParticleMaterial(Blocks.IRON_BLOCK.defaultBlockState());
        }
        return new Frame(material.sprite(),
            material.forceTranslucent() || material.sprite().transparency() == Transparency.TRANSLUCENT);
    }

    static void submit(Frame frame, List<PylonWire> wires, BlockPos origin, float ticks, PoseStack pose,
                       SubmitNodeCollector collector, int light) {
        if (frame != null) {
            var atlas = frame.sprite().atlasLocation();
            var type = frame.translucent() ? RenderTypes.entityTranslucent(atlas) : RenderTypes.entityCutout(atlas);
            collector.submitCustomGeometry(pose, type, (matrix, buffer) ->
                PylonDecorationMesh.frame(frame.sprite(), sink(matrix, buffer, light)));
        }
        if (!wires.isEmpty()) {
            collector.submitCustomGeometry(pose, RenderTypes.entityCutout(WHITE), (matrix, buffer) ->
                PylonDecorationMesh.wires(origin, wires, ticks, sink(matrix, buffer, light)));
        }
    }

    private static PylonDecorationMesh.VertexSink sink(PoseStack.Pose matrix, VertexConsumer buffer, int light) {
        return (x, y, z, u, v, color, nx, ny, nz) -> buffer.addVertex(matrix, x, y, z)
            .setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(matrix, nx, ny, nz);
    }
}
