package net.gasharaujo.superpylons.client;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;

final class PylonRenderTransforms {
    private PylonRenderTransforms() {}

    static void rotate(PoseStack pose, Quaternionf rotation) {
        pose.mulPose(rotation);
    }
}
