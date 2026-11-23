package net.gasharaujo.superpylons.block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;

final class PylonTestSerialization {
    static void load(SuperPylonBlockEntity entity, CompoundTag tag, MinecraftServer server) {
        entity.loadAdditional(TagValueInput.create(ProblemReporter.DISCARDING, server.registryAccess(), tag));
    }
}
