package net.gasharaujo.superpylons.block;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
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

    @Test
    void loadedTransferUpgradesPrecedeTheirResourceStorage(MinecraftServer server) {
        for (String direction : List.of("insert", "extract")) {
            var transfers = List.of(
                PylonUpgrade.byId(direction + "_item_upgrade"),
                PylonUpgrade.byId(direction + "_fluid_upgrade"),
                PylonUpgrade.byId(direction + "_energy_upgrade"),
                PylonUpgrade.byId(direction + "_xp_upgrade"));
            var original = pylon();
            original.loadUpgrades("energy_upgrade_6," + direction + "_item_upgrade," + direction +
                "_fluid_upgrade," + direction + "_energy_upgrade," + direction + "_xp_upgrade," +
                "water_generator_upgrade,pulverize_upgrade,xp_tank_upgrade,storage_upgrade_6");
            original.loadSpecialtyAmounts(123, 456);
            original.generateFluid(Fluids.WATER, 789);
            var loaded = pylon();
            PylonTestSerialization.load(loaded, original.getUpdateTag(server.registryAccess()), server);
            var installed = loaded.getInstalledUpgrades();
            assertEquals(transfers, installed.subList(0, 4));
            assertTrue(installed.indexOf(transfers.get(0)) < installed.indexOf(PylonUpgrade.STORAGE_6));
            assertTrue(installed.indexOf(transfers.get(1)) < installed.indexOf(PylonUpgrade.WATER_GENERATOR));
            assertTrue(installed.indexOf(transfers.get(2)) < installed.indexOf(PylonUpgrade.ENERGY_6));
            assertTrue(installed.indexOf(transfers.get(3)) < installed.indexOf(PylonUpgrade.XP_TANK));
            assertEquals(123, loaded.getExperience());
            assertEquals(456, loaded.getEnergy());
            assertEquals(789, loaded.getFluidAmount());
        }
    }

    private static SuperPylonBlockEntity pylon() {
        return new SuperPylonBlockEntity(BlockPos.ZERO, SuperPylonsMod.SUPER_PYLON.get().defaultBlockState());
    }
}
