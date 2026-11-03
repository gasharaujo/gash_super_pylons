package net.gasharaujo.superpylons.block;

import net.gasharaujo.superpylons.SuperPylonsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The animation is calculated on the client; no server ticker or packets are needed. */
public final class SuperPylonBlockEntity extends BlockEntity {
    public SuperPylonBlockEntity(BlockPos pos, BlockState state) {
        super(SuperPylonsMod.SUPER_PYLON_ENTITY.get(), pos, state);
    }
}
