package net.gasharaujo.superpylons.client;

import java.util.List;
import java.util.function.Function;
import net.gasharaujo.superpylons.block.PylonUpgrade;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

final class PylonUpgradeStamps {
    private static final float HEIGHT = 2.0F / 16.0F;
    private static final float START_RIGHT = 7.75F / 16.0F;
    private static final float SPACING = 2.0F / 16.0F;
    private static final float FACE_DEPTH = 0.5F + 1.0F / 1024.0F;

    private PylonUpgradeStamps() {}

    static List<TextureAtlasSprite> sprites(List<PylonUpgrade> upgrades, Function<String, TextureAtlasSprite> lookup) {
        return upgrades.stream().map(upgrade -> {
            TextureAtlasSprite sprite = lookup.apply(upgrade.id());
            return sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())
                ? lookup.apply("upgrade_base") : sprite;
        }).toList();
    }

    static void draw(List<TextureAtlasSprite> sprites, VertexSink sink) {
        for (Direction normal : Direction.Plane.HORIZONTAL) {
            float faceX = 0.5F + normal.getStepX() * FACE_DEPTH;
            float faceZ = 0.5F + normal.getStepZ() * FACE_DEPTH;
            // The right axis is relative to a viewer facing the outside of this side.
            int rightX = normal.getStepZ();
            int rightZ = -normal.getStepX();
            for (int i = 0; i < sprites.size(); i++) {
                TextureAtlasSprite sprite = sprites.get(i);
                float right = START_RIGHT - i * SPACING;
                float left = right - HEIGHT;
                float x0 = faceX + left * rightX;
                float x1 = faceX + right * rightX;
                float z0 = faceZ + left * rightZ;
                float z1 = faceZ + right * rightZ;
                sink.vertex(x0, 0, z0, normal, sprite.getU0(), sprite.getV1());
                sink.vertex(x1, 0, z1, normal, sprite.getU1(), sprite.getV1());
                sink.vertex(x1, HEIGHT, z1, normal, sprite.getU1(), sprite.getV0());
                sink.vertex(x0, HEIGHT, z0, normal, sprite.getU0(), sprite.getV0());
            }
        }
    }

    @FunctionalInterface
    interface VertexSink {
        void vertex(float x, float y, float z, Direction normal, float u, float v);
    }
}
