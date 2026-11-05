package net.gasharaujo.superpylons.client;

import net.createmod.catnip.api.math.Pointing;
import net.createmod.ponder.api.client.PonderPalette;
import net.createmod.ponder.api.client.registration.PonderPlugin;
import net.createmod.ponder.api.client.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.client.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.api.client.scene.SceneBuilder;
import net.createmod.ponder.api.client.scene.SceneBuildingUtil;
import net.createmod.ponder.api.client.PonderIndex;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.gasharaujo.superpylons.block.SuperPylonBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

final class PylonPonder implements PonderPlugin {
    private static final String SCHEMATIC = "pylon_basics";
    private static final String TAG = "super_pylons";
    private static final BlockPos PYLON = new BlockPos(1, 2, 2);
    private static final BlockPos CHEST = PYLON.below();
    private static final BlockPos OTHER = new BlockPos(3, 2, 2);

    static void register() { PonderIndex.addPlugin(new PylonPonder()); }

    @Override public String getModId() { return SuperPylonsMod.MOD_ID; }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<Identifier> helper) {
        var tag = helper.asIdentifier(TAG);
        for (var chapter : PylonPonderContent.CHAPTERS) {
            helper.addStoryBoard(helper.asIdentifier("super_pylon"), SCHEMATIC,
                (scene, util) -> build(scene, util, chapter), tag);
            for (String item : chapter.items()) {
                helper.addStoryBoard(helper.asIdentifier(item), SCHEMATIC, (scene, util) -> build(scene, util, chapter), tag);
            }
        }
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<Identifier> helper) {
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
        scene.title(chapter.id(), chapter.title());
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);
        if (chapter.chest()) scene.world().showSection(util.select().position(CHEST), Direction.DOWN);
        scene.world().showSection(util.select().position(PYLON), Direction.DOWN);
        if (chapter.other()) scene.world().showSection(util.select().position(OTHER), Direction.DOWN);
        scene.idle(10);
        for (var step : chapter.steps()) {
            BlockPos pos = switch (step.focus()) {
                case PYLON -> PYLON;
                case CHEST -> CHEST;
                case OTHER -> OTHER;
            };
            Vec3 center = Vec3.atCenterOf(pos);
            int time = Math.max(70, 35 + step.text().length() / 2);
            if (!step.icon().isEmpty() || step.action() != PylonPonderContent.Action.HOLD) {
                var control = scene.overlay().showControls(center.add(0, .55, 0), Pointing.DOWN, 38);
                if (!step.icon().isEmpty()) control.withItem(new ItemStack(BuiltInRegistries.ITEM.getValue(itemId(step.icon()))));
                switch (step.action()) {
                    case RIGHT -> control.rightClick();
                    case LEFT -> control.leftClick();
                    case SNEAK_RIGHT -> control.rightClick().whileSneaking();
                    case SNEAK_LEFT -> control.leftClick().whileSneaking();
                    case SCROLL -> control.scroll().whileSneaking();
                    default -> { }
                }
            }
            scene.overlay().showText(time).attachKeyFrame().pointAt(center).text(step.text());
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
                        item.putInt("count", 1);
                        tag.put("stored_item", item);
                    }
                });
                if (chapter.id().equals("wires") && pos.equals(OTHER)) {
                    scene.overlay().showLine(PonderPalette.MEDIUM, Vec3.atCenterOf(PYLON), center, time - 15);
                }
                scene.idle(time - 15);
            } else {
                scene.idle(time);
            }
            scene.idle(8);
        }
        scene.markAsFinished();
    }

    private static Identifier itemId(String path) {
        return Identifier.tryParse(path.contains(":") ? path : SuperPylonsMod.MOD_ID + ":" + path);
    }
}
