package net.gasharaujo.superpylons.client;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

record PylonFluidCore(TextureAtlasSprite sprite, int tint, int light, boolean translucent) {
    private static final float MAX = PylonCoreModel.HALF_SIZE / 16.0F;
    private static final float MIN = -MAX;
    private static final Corner BOTTOM_NORTHWEST = new Corner(MIN, MIN, MIN);
    private static final Corner BOTTOM_NORTHEAST = new Corner(MAX, MIN, MIN);
    private static final Corner BOTTOM_SOUTHWEST = new Corner(MIN, MIN, MAX);
    private static final Corner BOTTOM_SOUTHEAST = new Corner(MAX, MIN, MAX);
    private static final Corner TOP_NORTHWEST = new Corner(MIN, MAX, MIN);
    private static final Corner TOP_NORTHEAST = new Corner(MAX, MAX, MIN);
    private static final Corner TOP_SOUTHWEST = new Corner(MIN, MAX, MAX);
    private static final Corner TOP_SOUTHEAST = new Corner(MAX, MAX, MAX);

    private static final Face[] FACES = {
        new Face(Direction.DOWN, BOTTOM_SOUTHEAST, BOTTOM_SOUTHWEST, BOTTOM_NORTHWEST, BOTTOM_NORTHEAST),
        new Face(Direction.UP, TOP_NORTHEAST, TOP_NORTHWEST, TOP_SOUTHWEST, TOP_SOUTHEAST),
        new Face(Direction.WEST, TOP_SOUTHWEST, TOP_NORTHWEST, BOTTOM_NORTHWEST, BOTTOM_SOUTHWEST),
        new Face(Direction.NORTH, TOP_NORTHWEST, TOP_NORTHEAST, BOTTOM_NORTHEAST, BOTTOM_NORTHWEST),
        new Face(Direction.EAST, TOP_NORTHEAST, TOP_SOUTHEAST, BOTTOM_SOUTHEAST, BOTTOM_NORTHEAST),
        new Face(Direction.SOUTH, TOP_SOUTHEAST, TOP_SOUTHWEST, BOTTOM_SOUTHWEST, BOTTOM_SOUTHEAST)
    };

    void draw(VertexSink sink) {
        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();
        for (Face face : FACES) {
            sink.vertex(face.a, face.normal, u0, v0);
            sink.vertex(face.b, face.normal, u1, v0);
            sink.vertex(face.c, face.normal, u1, v1);
            sink.vertex(face.d, face.normal, u0, v1);
        }
    }

    @FunctionalInterface
    interface VertexSink {
        void vertex(Corner corner, Direction normal, float u, float v);
    }

    record Corner(float x, float y, float z) {}
    private record Face(Direction normal, Corner a, Corner b, Corner c, Corner d) {}
}
