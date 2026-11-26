package net.gasharaujo.superpylons.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.gasharaujo.superpylons.block.PylonUpgrade;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class PylonUpgradeStamps {
    private static final float HEIGHT = 2.0F / 16.0F;
    private static final float START_RIGHT = 7.75F / 16.0F;
    private static final float FACE_DEPTH = 0.5F + 1.0F / 1024.0F;

    private PylonUpgradeStamps() {}

    record Stamp<T>(TextureAtlasSprite sprite, T model) {}

    static <T> List<Stamp<T>> stamps(List<PylonUpgrade> upgrades, ItemStack furnace,
                                     Function<String, TextureAtlasSprite> lookup, Function<PylonUpgrade, T> model) {
        var stamps = new ArrayList<Stamp<T>>();
        for (PylonUpgrade upgrade : upgrades) {
            if (upgrade == PylonUpgrade.SMELT && !furnace.isEmpty()) {
                stamps.add(new Stamp<>(lookup.apply(furnace.is(Items.BLAST_FURNACE)
                    ? "minecraft:block/blast_furnace_front" : "minecraft:block/smoker_front"), null));
            }
            if (upgrade == PylonUpgrade.REDSTONE || upgrade == PylonUpgrade.REDSTONE_LEVEL || upgrade == PylonUpgrade.GRINDSTONE) {
                stamps.add(new Stamp<>(null, model.apply(upgrade)));
                continue;
            }
            TextureAtlasSprite sprite = lookup.apply(upgrade == PylonUpgrade.CREATIVE_STORAGE ?
                "minecraft:block/command_block_front" : "gash_super_pylons:item/" + upgrade.id());
            if (sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
                sprite = lookup.apply("gash_super_pylons:item/upgrade_base");
            }
            stamps.add(new Stamp<>(sprite, null));
        }
        return stamps;
    }

    static int toolSlot(boolean hasTool, boolean hasSuper) {
        if (!hasTool) return -1;
        return hasSuper ? 1 : 0;
    }

    static int shelfSlot(List<PylonUpgrade> upgrades, int toolSlot, boolean hasFurnace) {
        int slot = upgrades.indexOf(PylonUpgrade.ENCHANTING);
        if (slot < 0) return -1;
        int smelt = upgrades.indexOf(PylonUpgrade.SMELT);
        if (hasFurnace && smelt >= 0 && smelt < slot) slot++;
        return slot + (toolSlot >= 0 && slot >= toolSlot ? 1 : 0);
    }

    static int stampSlot(int index, int toolSlot, int shelfSlot, int shelves) {
        int slot = index + (toolSlot >= 0 && index >= toolSlot ? 1 : 0);
        return shelfSlot >= 0 && slot >= shelfSlot ? slot + shelves : slot;
    }

    static void draw(List<? extends Stamp<?>> stamps, int toolSlot, int shelfSlot, int shelves, VertexSink sink) {
        float width = slotWidth(stamps.size() + (toolSlot >= 0 ? 1 : 0) + shelves);
        for (Direction normal : Direction.Plane.HORIZONTAL) {
            float faceX = 0.5F + normal.getStepX() * FACE_DEPTH;
            float faceZ = 0.5F + normal.getStepZ() * FACE_DEPTH;
            // The right axis is relative to a viewer facing the outside of this side.
            int rightX = normal.getStepZ();
            int rightZ = -normal.getStepX();
            for (int i = 0; i < stamps.size(); i++) {
                TextureAtlasSprite sprite = stamps.get(i).sprite();
                if (sprite == null) continue;
                int slot = stampSlot(i, toolSlot, shelfSlot, shelves);
                float right = START_RIGHT - slot * width;
                float left = right - width;
                float x0 = faceX + left * rightX;
                float x1 = faceX + right * rightX;
                float z0 = faceZ + left * rightZ;
                float z1 = faceZ + right * rightZ;
                sink.vertex(x0, 0, z0, normal, sprite.getU0(), sprite.getV1());
                sink.vertex(x1, 0, z1, normal, sprite.getU1(), sprite.getV1());
                sink.vertex(x1, width, z1, normal, sprite.getU1(), sprite.getV0());
                sink.vertex(x0, width, z0, normal, sprite.getU0(), sprite.getV0());
            }
        }
    }

    static void placeItem(PoseStack pose, Direction face, int total, int slot) {
        float width = slotWidth(total);
        float right = START_RIGHT - (slot + 0.5F) * width;
        float depth = FACE_DEPTH + 1.0F / 128;
        pose.translate(0.5F + face.getStepX() * depth + face.getStepZ() * right, width / 2,
            0.5F + face.getStepZ() * depth - face.getStepX() * right);
        PylonRenderTransforms.rotate(pose, Axis.YP.rotationDegrees(-face.toYRot()));
        pose.scale(width, width, width);
    }

    private static float slotWidth(int slots) {
        return Math.min(HEIGHT, 2 * START_RIGHT / Math.max(1, slots));
    }

    @FunctionalInterface
    interface VertexSink {
        void vertex(float x, float y, float z, Direction normal, float u, float v);
    }
}
