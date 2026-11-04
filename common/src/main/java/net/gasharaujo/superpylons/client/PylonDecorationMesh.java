package net.gasharaujo.superpylons.client;

import java.util.List;
import net.gasharaujo.superpylons.block.PylonFrame;
import net.gasharaujo.superpylons.block.PylonWire;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

final class PylonDecorationMesh {
    private static final int WIRE_SEGMENTS = 12;
    private static final double WIRE_HALF_WIDTH = 1.0 / 64.0;

    private PylonDecorationMesh() {}

    static void frame(TextureAtlasSprite sprite, VertexSink sink) {
        for (var bar : PylonFrame.BARS) {
            Vec3 a = new Vec3(bar.minX, bar.minY, bar.minZ);
            Vec3 b = new Vec3(bar.maxX, bar.minY, bar.minZ);
            Vec3 c = new Vec3(bar.minX, bar.maxY, bar.minZ);
            Vec3 d = new Vec3(bar.maxX, bar.maxY, bar.minZ);
            Vec3 e = new Vec3(bar.minX, bar.minY, bar.maxZ);
            Vec3 f = new Vec3(bar.maxX, bar.minY, bar.maxZ);
            Vec3 g = new Vec3(bar.minX, bar.maxY, bar.maxZ);
            Vec3 h = new Vec3(bar.maxX, bar.maxY, bar.maxZ);
            face(sink, sprite, b, a, c, d);
            face(sink, sprite, e, f, h, g);
            face(sink, sprite, a, e, g, c);
            face(sink, sprite, f, b, d, h);
            face(sink, sprite, c, g, h, d);
            face(sink, sprite, a, b, f, e);
        }
    }

    private static void face(VertexSink sink, TextureAtlasSprite sprite, Vec3 a, Vec3 b, Vec3 c, Vec3 d) {
        Vec3 normal = b.subtract(a).cross(c.subtract(a)).normalize();
        frameVertex(sink, sprite, a, normal);
        frameVertex(sink, sprite, b, normal);
        frameVertex(sink, sprite, c, normal);
        frameVertex(sink, sprite, d, normal);
    }

    private static void frameVertex(VertexSink sink, TextureAtlasSprite sprite, Vec3 pos, Vec3 normal) {
        double u = normal.x != 0 ? pos.z : pos.x;
        double v = normal.y != 0 ? pos.z : 1 - pos.y;
        vertex(sink, pos, normal, (float) (sprite.getU0() + (sprite.getU1() - sprite.getU0()) * u),
            (float) (sprite.getV0() + (sprite.getV1() - sprite.getV0()) * v), -1);
    }

    static void wires(BlockPos origin, List<PylonWire> wires, float ticks, VertexSink sink) {
        Vec3 start = new Vec3(0.5, PylonAnimation.coreHeight(ticks), 0.5);
        for (PylonWire wire : wires) {
            BlockPos target = wire.target();
            Vec3 offset = new Vec3(target.getX() - origin.getX(), target.getY() - origin.getY(), target.getZ() - origin.getZ());
            Vec3 side = new Vec3(-offset.z, 0, offset.x).normalize();
            if (side.lengthSqr() == 0) side = new Vec3(1, 0, 0);
            Vec3 cross = offset.cross(side).normalize().scale(WIRE_HALF_WIDTH);
            side = side.scale(WIRE_HALF_WIDTH);
            double sag = Math.min(1.25, offset.horizontalDistance() * 0.08);
            Vec3 last = start;
            for (int i = 1; i <= WIRE_SEGMENTS; i++) {
                double t = (double) i / WIRE_SEGMENTS;
                Vec3 point = start.add(offset.scale(t)).add(0, -4 * sag * t * (1 - t), 0);
                ribbon(sink, last, point, side, wire.type().color());
                ribbon(sink, last, point, cross, wire.type().color());
                last = point;
            }
        }
    }

    private static void ribbon(VertexSink sink, Vec3 a, Vec3 b, Vec3 width, int color) {
        Vec3 normal = b.subtract(a).cross(width).normalize();
        Vec3 a0 = a.subtract(width), a1 = a.add(width);
        Vec3 b0 = b.subtract(width), b1 = b.add(width);
        vertex(sink, a0, normal, 0, 0, color);
        vertex(sink, b0, normal, 1, 0, color);
        vertex(sink, b1, normal, 1, 1, color);
        vertex(sink, a1, normal, 0, 1, color);
        Vec3 back = normal.scale(-1);
        vertex(sink, a1, back, 0, 1, color);
        vertex(sink, b1, back, 1, 1, color);
        vertex(sink, b0, back, 1, 0, color);
        vertex(sink, a0, back, 0, 0, color);
    }

    private static void vertex(VertexSink sink, Vec3 pos, Vec3 normal, float u, float v, int color) {
        sink.vertex((float) pos.x, (float) pos.y, (float) pos.z, u, v, color,
            (float) normal.x, (float) normal.y, (float) normal.z);
    }

    @FunctionalInterface
    interface VertexSink {
        void vertex(float x, float y, float z, float u, float v, int color, float nx, float ny, float nz);
    }
}
