package net.gasharaujo.superpylons.block;

import static org.junit.jupiter.api.Assertions.*;

import net.gasharaujo.superpylons.SuperPylonsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(EphemeralTestServerProvider.class)
final class PylonGeneratorTest {
    @Test
    void legacyGeneratorIdsAndUtilitySurviveSaveAndReloadTogether(MinecraftServer server) {
        var pylon = pylon();
        pylon.loadUpgrades("cobblestone_generator_upgrade,dropper_upgrade,pulverize_upgrade,storage_upgrade_2");
        pylon.contents.restore(new ItemStack(Items.COBBLESTONE), 123, pylon.getItemCapacity());
        var loaded = pylon();
        PylonTestSerialization.load(loaded, pylon.getUpdateTag(server.registryAccess()), server);
        assertEquals(PylonUpgrade.COBBLESTONE_GENERATOR, loaded.getGenerator());
        assertEquals(PylonUpgrade.DROPPER, loaded.getSpecialty());
        assertEquals(PylonUpgrade.PULVERIZE, loaded.getProcessing());
        assertEquals(123, loaded.getStoredCount());
        assertEquals(8192, loaded.getItemCapacity());
        assertTrue(loaded.getInstalledUpgrades().indexOf(PylonUpgrade.COBBLESTONE_GENERATOR) <
            loaded.getInstalledUpgrades().indexOf(PylonUpgrade.PULVERIZE));
    }

    @Test
    void fluidGeneratorAndXpTankPreserveIndependentBalances(MinecraftServer server) {
        var pylon = pylon();
        pylon.loadUpgrades("xp_tank_upgrade,water_generator_upgrade");
        pylon.loadSpecialtyAmounts(123, 0);
        pylon.generateFluid(Fluids.WATER, 100);
        var loaded = pylon();
        PylonTestSerialization.load(loaded, pylon.getUpdateTag(server.registryAccess()), server);
        assertTrue(loaded.isTank());
        assertTrue(loaded.isXpTank());
        assertEquals(123, loaded.getExperience());
        assertEquals(100, loaded.getFluidAmount());
        assertEquals(32000, loaded.getFluidCapacity());
        assertTrue(loaded.canFillFluid(Fluids.WATER));
        assertFalse(loaded.canFillFluid(Fluids.LAVA));
    }

    private static SuperPylonBlockEntity pylon() {
        return new SuperPylonBlockEntity(BlockPos.ZERO, SuperPylonsMod.SUPER_PYLON.get().defaultBlockState());
    }
}
