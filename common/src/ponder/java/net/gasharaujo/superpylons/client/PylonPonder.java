package net.gasharaujo.superpylons.client;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.foundation.PonderIndex;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

final class PylonPonder implements PonderPlugin {
    static void register() { PonderIndex.addPlugin(new PylonPonder()); }

    @Override public String getModId() { return SuperPylonsMod.MOD_ID; }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.addStoryBoard(BuiltInRegistries.ITEM.getKey(SuperPylonsMod.SUPER_PYLON_ITEM.get()),
            "pylon_basics", PylonPonder::pylonBasics);
    }

    private static void pylonBasics(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("pylon_basics", "Storing and moving resources");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(1, 1, 2, 1, 2, 2), Direction.DOWN);
        scene.overlay().showText(65).attachKeyFrame().pointAt(util.vector().centerOf(1, 2, 2))
            .text("Right-click to store items in the Super Pylon.");
        scene.idle(75);
        scene.world().showSection(util.select().position(3, 2, 2), Direction.DOWN);
        scene.overlay().showText(90).attachKeyFrame().pointAt(util.vector().centerOf(3, 2, 2))
            .text("Use a Wire on the source, then the destination. Select both again to recover the Wire.");
        scene.idle(100);
        scene.markAsFinished();
    }
}
