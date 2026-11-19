package net.gasharaujo.superpylons.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/** Coordinates use the original Blockbench model, where one block is 16 units. */
public final class PylonAnimation {
    public static final float ROTATION_TICKS = 160.0F;
    public static final float BOB_TICKS = 80.0F;
    // The interior spans model units 2 to 14, and the core travels one eighth of that height.
    public static final float BOB_AMPLITUDE = (12.0F / 8.0F / 2.0F) / 16.0F;

    private PylonAnimation() {}

    public static float ticks(long gameTime, float partialTick) {
        return (gameTime % (long) ROTATION_TICKS) + partialTick;
    }

    public static float itemTicks(long milliseconds) {
        return (milliseconds % 8000L) / 50.0F;
    }

    public static void apply(PoseStack pose, float ticks) {
        applyOrbit(pose, ticks);
        // Preserve the core's Z/Y/X Euler rotation from Pylon.json about its own center.
        pose.mulPose(Axis.ZP.rotationDegrees(-125.0F));
        pose.mulPose(Axis.YP.rotationDegrees(45.0F));
        pose.mulPose(Axis.XP.rotationDegrees(180.0F));
    }

    public static void applyOrbit(PoseStack pose, float ticks) {
        pose.translate(0.5, coreHeight(ticks), 0.5);
        // Positive Y rotation is counterclockwise when viewed from above.
        pose.mulPose(Axis.YP.rotationDegrees(ticks * 360.0F / ROTATION_TICKS));
    }

    static double coreHeight(float ticks) {
        return 0.5 + Math.sin(ticks * Math.PI * 2.0 / BOB_TICKS) * BOB_AMPLITUDE;
    }

    static final int XP_ORB_COUNT = 8;
    static final int SUPER_STAR_COUNT = 4;

    static void placeStar(PoseStack pose, float ticks, int star, Quaternionf camera) {
        double angle = 2 * Math.PI * (ticks / ROTATION_TICKS + (double) star / SUPER_STAR_COUNT);
        pose.translate(0.5 + Math.cos(angle) * 0.32, coreHeight(ticks) + Math.sin(angle) * 0.13,
            0.5 - Math.sin(angle) * 0.32);
        pose.mulPose(camera);
        pose.scale(0.19F, 0.19F, 0.19F);
    }

    static final int HUB_CHESTS_PER_RING = 6;
    static final float HUB_CENTER_SCALE = 0.35F;

    enum HubRing {
        HORIZONTAL(0.28, 0.105F, Math.PI / 6, 1, 1, 0),
        VERTICAL(0.28, 0.105F, 0, 1, 0, -1),
        DIAGONAL_CLOCKWISE(0.41, 0.15F, 0, 1, Math.sqrt(0.5), Math.sqrt(0.5)),
        DIAGONAL_COUNTERCLOCKWISE(0.41, 0.15F, Math.PI / 6, -1, -Math.sqrt(0.5), Math.sqrt(0.5));

        final double radius;
        final float scale;
        final double phase;
        final int direction;
        final double axisX;
        final double axisY;

        HubRing(double radius, float scale, double phase, int direction, double axisX, double axisY) {
            this.radius = radius;
            this.scale = scale;
            this.phase = phase;
            this.direction = direction;
            this.axisX = axisX;
            this.axisY = axisY;
        }

        Vec3 position(float ticks, int chest) {
            double angle = 2 * Math.PI * (ticks / ROTATION_TICKS + (double) chest / HUB_CHESTS_PER_RING) + phase;
            double transverse = radius * Math.cos(angle);
            return new Vec3(0.5 + transverse * axisX, 0.5 + transverse * axisY,
                0.5 + direction * radius * Math.sin(angle));
        }
    }

    static void placeHubCenter(PoseStack pose, float ticks) {
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-ticks * 360 / ROTATION_TICKS));
        pose.scale(HUB_CENTER_SCALE, HUB_CENTER_SCALE, HUB_CENTER_SCALE);
    }

    static void placeHubChest(PoseStack pose, float ticks, HubRing ring, int chest) {
        // Each pair of rings shares a radius and crosses on the Z axis. A half-step offset keeps
        // their chests separated there. The two pairs are separated by their radii everywhere.
        Vec3 position = ring.position(ticks, chest);
        pose.translate(position.x, position.y, position.z);
        pose.mulPose(Axis.YP.rotationDegrees(ring.ordinal() * 90));
        pose.scale(ring.scale, ring.scale, ring.scale);
    }

    static Vec3 xpOrbit(float ticks, int orb) {
        double angle = ticks * Math.PI * 2 / ROTATION_TICKS * (orb + 2) + orb * 2.4;
        double radius = 0.41 + 0.09 * Math.sin(angle * 2 + orb);
        return new Vec3(0.5 + Math.cos(angle) * radius,
            coreHeight(ticks) + 0.25 * Math.sin(angle + orb), 0.5 + Math.sin(angle) * radius);
    }
}
