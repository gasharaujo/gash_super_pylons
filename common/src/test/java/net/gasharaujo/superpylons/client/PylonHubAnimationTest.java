package net.gasharaujo.superpylons.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class PylonHubAnimationTest {
    private static final Vec3 CENTER = new Vec3(0.5, 0.5, 0.5);

    @Test
    void allTwentyFourChestsClearTheCenterFrameAndEachOtherThroughoutAnOrbit() {
        var rings = PylonAnimation.HubRing.values();
        int perRing = PylonAnimation.HUB_CHESTS_PER_RING;
        assertEquals(4, rings.length);
        assertEquals(6, perRing);

        // FIXED scales a chest model by 0.5. Enclose its full unit cube to cover all rotations.
        double vanillaChestRadius = Math.sqrt(3) / 4;
        double centerRadius = PylonAnimation.HUB_CENTER_SCALE * vanillaChestRadius;
        double fastest = 0;
        for (var ring : rings) {
            double chestRadius = ring.scale * vanillaChestRadius;
            assertTrue(ring.radius > centerRadius + chestRadius);
            // The whole orbit stays inside the block and short of any of the edge bars.
            assertTrue(ring.radius + chestRadius < 0.5);
            fastest = Math.max(fastest, 2 * Math.PI * ring.radius / PylonAnimation.ROTATION_TICKS);
        }

        // At any rendered partial tick, the nearest sample is at most half an interval away.
        // Two chests can close their gap by at most twice the fastest speed over that time.
        float step = 0.25F;
        double interpolationMargin = fastest * step;
        var positions = new Vec3[rings.length * perRing];
        for (float ticks = 0; ticks <= PylonAnimation.ROTATION_TICKS; ticks += step) {
            for (int index = 0; index < positions.length; index++) {
                positions[index] = rings[index / perRing].position(ticks, index % perRing);
                assertEquals(rings[index / perRing].radius, positions[index].distanceTo(CENTER), 1e-7);
            }
            for (int first = 0; first < positions.length; first++) {
                for (int second = first + 1; second < positions.length; second++) {
                    double radii = vanillaChestRadius * (rings[first / perRing].scale + rings[second / perRing].scale);
                    assertTrue(positions[first].distanceTo(positions[second]) > radii + interpolationMargin,
                        "Overlap near tick " + ticks + " between chests " + first + " and " + second);
                }
            }
        }
    }
}
