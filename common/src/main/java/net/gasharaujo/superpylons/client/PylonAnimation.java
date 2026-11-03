package net.gasharaujo.superpylons.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

/** Coordinates use the original Blockbench model, where one block is 16 units. */
public final class PylonAnimation {
    public static final float ROTATION_TICKS = 160.0F;
    public static final float BOB_TICKS = 80.0F;
    // Interior height = 12 - 2 = 10. Total travel is 10 / 8 = 1.25 model units.
    public static final float BOB_AMPLITUDE = (10.0F / 8.0F / 2.0F) / 16.0F;

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
        double bob = Math.sin(ticks * Math.PI * 2.0 / BOB_TICKS) * BOB_AMPLITUDE;
        pose.translate(0.5, 7.0 / 16.0 + bob, 0.5);
        // Positive Y rotation is counterclockwise when viewed from above.
        pose.mulPose(Axis.YP.rotationDegrees(ticks * 360.0F / ROTATION_TICKS));
    }
}
