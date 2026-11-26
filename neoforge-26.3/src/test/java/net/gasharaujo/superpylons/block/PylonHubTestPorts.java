package net.gasharaujo.superpylons.block;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class PylonHubTestPorts {
    private PylonHubTestPorts() {}

    static void transfer(ServerLevel level, BlockPos pos, SuperPylonBlockEntity water, SuperPylonBlockEntity battery) {
        var items = level.getCapability(Capabilities.Item.BLOCK, pos, Direction.NORTH);
        var fluids = level.getCapability(Capabilities.Fluid.BLOCK, pos, Direction.NORTH);
        var energy = level.getCapability(Capabilities.Energy.BLOCK, pos, Direction.NORTH);
        assertNotNull(items);
        assertNotNull(fluids);
        assertNotNull(energy);
        assertSame(Items.IRON_INGOT, items.getResource(1).getItem());
        assertEquals((long) Integer.MAX_VALUE + 7, items.getAmountAsLong(1));
        var coldWater = FluidResource.of(Fluids.WATER);
        try (var trial = Transaction.openRoot()) {
            assertEquals(250, fluids.insert(4, coldWater, 250, trial));
            assertEquals(100, energy.insert(100, trial));
        }
        assertEquals(0, water.getFluidAmount());
        assertEquals(0, battery.getEnergy());
        try (var transfer = Transaction.openRoot()) {
            assertEquals(250, fluids.insert(4, coldWater, 250, transfer));
            assertEquals(100, energy.insert(100, transfer));
            transfer.commit();
        }
        assertEquals(250, water.getFluidAmount());
        assertEquals(100, battery.getEnergy());
    }
}
