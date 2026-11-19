package net.gasharaujo.superpylons.block;

import net.minecraft.world.level.material.EmptyFluid;
import net.minecraft.world.level.material.FluidState;

/** A source fluid for tanks and pipes, with no block form in the world. */
abstract class PylonLiquidExperience extends EmptyFluid {
    static final String NAME = "liquid_experience";

    @Override protected boolean isEmpty() { return false; }
    @Override public boolean isSource(FluidState state) { return true; }
    @Override public int getAmount(FluidState state) { return 8; }
}
