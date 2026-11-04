package net.gasharaujo.superpylons.block;

import java.util.List;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;

public final class PylonFrame {
    public static final BooleanProperty PAINTED = BooleanProperty.create("painted");
    public static final List<AABB> BARS = List.of(
        box(0, 0, 0, 2, 2, 16), box(14, 0, 0, 16, 2, 16),
        box(0, 14, 0, 2, 16, 16), box(14, 14, 0, 16, 16, 16),
        box(0, 2, 0, 2, 14, 2), box(14, 2, 0, 16, 14, 2),
        box(0, 2, 14, 2, 14, 16), box(14, 2, 14, 16, 14, 16),
        box(2, 0, 0, 14, 2, 2), box(2, 0, 14, 14, 2, 16),
        box(2, 14, 0, 14, 16, 2), box(2, 14, 14, 14, 16, 16)
    );

    private PylonFrame() {}

    private static AABB box(int x0, int y0, int z0, int x1, int y1, int z1) {
        return new AABB(x0 / 16.0, y0 / 16.0, z0 / 16.0, x1 / 16.0, y1 / 16.0, z1 / 16.0);
    }
}
