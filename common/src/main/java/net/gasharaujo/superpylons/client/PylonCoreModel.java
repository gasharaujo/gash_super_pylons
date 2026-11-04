package net.gasharaujo.superpylons.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

public final class PylonCoreModel {
    static final float HALF_SIZE = 2.5F;

    private PylonCoreModel() {}

    public static ModelPart create() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("core",
            CubeListBuilder.create().texOffs(0, 0)
                .addBox(-HALF_SIZE, -HALF_SIZE, -HALF_SIZE, 2 * HALF_SIZE, 2 * HALF_SIZE, 2 * HALF_SIZE),
            PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32).bakeRoot();
    }
}
