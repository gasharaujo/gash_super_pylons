package net.gasharaujo.superpylons.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.phys.Vec3;

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

    static Vec3 xpOrbit(float ticks, int orb) {
        double angle = ticks * Math.PI * 2 / ROTATION_TICKS * (orb + 2) + orb * 2.4;
        double radius = 0.22 + 0.055 * Math.sin(angle * 2 + orb);
        return new Vec3(0.5 + Math.cos(angle) * radius,
            coreHeight(ticks) + 0.14 * Math.sin(angle + orb), 0.5 + Math.sin(angle) * radius);
    }
}
