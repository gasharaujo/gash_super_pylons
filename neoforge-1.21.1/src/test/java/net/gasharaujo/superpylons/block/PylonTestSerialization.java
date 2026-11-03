package net.gasharaujo.superpylons.block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;


final class PylonTestSerialization {
    static void load(SuperPylonBlockEntity entity, CompoundTag tag, MinecraftServer server) {
        entity.loadAdditional(tag, server.registryAccess());
    }
}
