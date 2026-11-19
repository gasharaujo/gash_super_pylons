package net.gasharaujo.superpylons.block;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

final class PylonHubTestPorts {
    private PylonHubTestPorts() {}

    static void transfer(ServerLevel level, BlockPos pos, SuperPylonBlockEntity water, SuperPylonBlockEntity battery) {
        var items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.NORTH);
        var fluids = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.NORTH);
        var energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, Direction.NORTH);
        assertNotNull(items);
        assertNotNull(fluids);
        assertNotNull(energy);
        assertTrue(items.getStackInSlot(1).is(Items.IRON_INGOT));
        assertEquals(Integer.MAX_VALUE, items.getStackInSlot(1).getCount());
        var coldWater = new FluidStack(Fluids.WATER, 250);
        assertEquals(250, fluids.fill(coldWater, FluidAction.SIMULATE));
        assertEquals(100, energy.receiveEnergy(100, true));
        assertEquals(0, water.getFluidAmount());
        assertEquals(0, battery.getEnergy());
        assertEquals(250, fluids.fill(coldWater, FluidAction.EXECUTE));
        assertEquals(100, energy.receiveEnergy(100, false));
        assertEquals(250, water.getFluidAmount());
        assertEquals(100, battery.getEnergy());
    }
}
