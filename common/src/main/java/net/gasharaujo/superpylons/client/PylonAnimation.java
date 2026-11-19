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

    static void placeHubChest(PoseStack pose, float ticks, int orbit) {
        if (orbit == -1) {
            pose.translate(0.5, 0.5, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(-ticks * 360 / ROTATION_TICKS));
            pose.scale(0.35F, 0.35F, 0.35F);
            return;
        }
        double phase = switch (orbit) {
            case 0 -> 0.5;
            case 2 -> 0.25;
            case 3 -> 0.77;
            default -> 0;
        };
        double angle = Math.PI * (ticks * 2 / ROTATION_TICKS + phase);
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        double x = 0, y = 0, z = 0;
        double radius = 0.28;
        switch (orbit) {
            case 0 -> { x = cos; z = sin; }
            case 1 -> { y = -cos; z = sin; }
            case 2 -> { x = cos * 0.7071; z = sin; y = cos * 0.7071; }
            case 3 -> { x = -cos * 0.7071; z = -sin; y = cos * 0.7071; }
            default -> throw new IllegalArgumentException("Unknown hub orbit: " + orbit);
        }
        pose.translate(0.5 + radius * x, 0.5 + radius * y, 0.5 + radius * z);
        pose.mulPose(Axis.YP.rotationDegrees(orbit * 90));
        pose.scale(0.17F, 0.17F, 0.17F);
    }

    static Vec3 xpOrbit(float ticks, int orb) {
        double angle = ticks * Math.PI * 2 / ROTATION_TICKS * (orb + 2) + orb * 2.4;
        double radius = 0.41 + 0.09 * Math.sin(angle * 2 + orb);
        return new Vec3(0.5 + Math.cos(angle) * radius,
            coreHeight(ticks) + 0.25 * Math.sin(angle + orb), 0.5 + Math.sin(angle) * radius);
    }
}
