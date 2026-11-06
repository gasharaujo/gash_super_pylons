package net.gasharaujo.superpylons.client;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.foundation.PonderIndex;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.gasharaujo.superpylons.block.SuperPylonBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

final class PylonPonder implements PonderPlugin {
    private static final String TAG = "super_pylons";

    static void register() { PonderIndex.addPlugin(new PylonPonder()); }

    @Override public String getModId() { return SuperPylonsMod.MOD_ID; }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        var tag = helper.asLocation(TAG);
        for (var chapter : PylonPonderContent.CHAPTERS) {
            helper.addStoryBoard(helper.asLocation("super_pylon"), chapter.layout().schematic,
                (scene, util) -> build(scene, util, chapter), tag);
            for (String item : chapter.items()) {
                helper.addStoryBoard(helper.asLocation(item), chapter.layout().schematic, (scene, util) -> build(scene, util, chapter), tag);
            }
        }
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.registerTag(TAG).title("Gash's Super Pylons")
            .description("Storage, upgrades, automation, wires and tools.")
            .idAsIcon().addToIndex().register();
        var tag = itemId(TAG);
        helper.addTagToComponent(itemId("super_pylon"), tag);
        for (var chapter : PylonPonderContent.CHAPTERS) {
            for (String item : chapter.items()) helper.addTagToComponent(itemId(item), tag);
        }
    }

    private static void build(SceneBuilder scene, SceneBuildingUtil util, PylonPonderContent.Chapter chapter) {
        var layout = chapter.layout();
        scene.title(chapter.id(), chapter.title());
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.85f);
        scene.showBasePlate();
        scene.idle(10);
        if (layout.chest != null) scene.world().showSection(util.select().position(layout.chest), Direction.DOWN);
        scene.world().showSection(util.select().position(layout.pylon), Direction.DOWN);
        if (layout.other != null) scene.world().showSection(util.select().position(layout.other), Direction.DOWN);
        scene.idle(10);
        for (var step : chapter.steps()) {
            BlockPos pos = switch (step.focus()) {
                case PYLON -> layout.pylon;
                case CHEST -> layout.chest;
                case OTHER -> layout.other;
            };
            Vec3 center = Vec3.atCenterOf(pos);
            int time = Math.max(70, 35 + step.text().length() / 2);
            if (!step.icon().isEmpty() || step.action() != PylonPonderContent.Action.HOLD) {
                var control = scene.overlay().showControls(center.add(0, .55, 0), Pointing.DOWN, 38);
                if (!step.icon().isEmpty()) control.withItem(new ItemStack(BuiltInRegistries.ITEM.get(itemId(step.icon()))));
                switch (step.action()) {
                    case RIGHT -> control.rightClick();
                    case LEFT -> control.leftClick();
                    case SNEAK_RIGHT -> control.rightClick().whileSneaking();
                    case SNEAK_LEFT -> control.leftClick().whileSneaking();
                    case SCROLL -> control.scroll().whileSneaking();
                    default -> { }
                }
            }
            scene.overlay().showText(time).attachKeyFrame().independent(58).text(step.text());
            if (step.display() != null) {
                scene.idle(15);
                var display = step.display();
                scene.world().modifyBlockEntityNBT(util.select().position(pos), SuperPylonBlockEntity.class, tag -> {
                    tag.putString("upgrades", display.upgrades());
                    tag.putLong("stored_count", display.count());
                    if (display.item().isEmpty()) {
                        tag.remove("stored_item");
                    } else {
                        var item = new CompoundTag();
                        item.putString("id", display.item());
                        item.putByte("Count", (byte) 1);
                        item.putInt("count", 1);
                        tag.put("stored_item", item);
                    }
                });
                if (chapter.id().equals("wires") && pos.equals(layout.other)) {
                    scene.overlay().showLine(PonderPalette.MEDIUM, Vec3.atCenterOf(layout.pylon), center, time - 15);
                }
                scene.idle(time - 15);
            } else {
                scene.idle(time);
            }
            scene.idle(8);
        }
        scene.markAsFinished();
    }

    private static ResourceLocation itemId(String path) {
        return ResourceLocation.tryParse(path.contains(":") ? path : SuperPylonsMod.MOD_ID + ":" + path);
    }
}
