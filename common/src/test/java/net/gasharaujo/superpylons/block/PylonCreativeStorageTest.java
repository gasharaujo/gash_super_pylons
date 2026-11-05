package net.gasharaujo.superpylons.block;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(EphemeralTestServerProvider.class)
final class PylonCreativeStorageTest {
    @Test
    void itemCopiesPreserveExactTemplateAndDisablingRestoresRealStock(MinecraftServer server) {
        AtomicBoolean creative = new AtomicBoolean(true);
        PylonContents source = new PylonContents(creative::get, () -> 2048);
        assertTrue(source.extract(64).isEmpty());
        ItemStack named = new ItemStack(Items.DIAMOND);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Original template"));
        source.restore(named, 1);
        for (int i = 0; i < 3; i++) {
            ItemStack extracted = source.extract(64);
            assertEquals(64, extracted.getCount());
            assertTrue(PylonItemRules.exactlyEqual(named, extracted));
            assertEquals(1, source.count());
        }
        PylonContents target = new PylonContents();
        assertEquals(500, source.moveTo(target, 500, 500));
        assertEquals(500, target.count());
        assertEquals(1, source.count());
        assertEquals(0, source.moveTo(target, 500, 500));
        PylonContents different = new PylonContents();
        different.restore(new ItemStack(Items.EMERALD), 1);
        assertEquals(0, source.moveTo(different, 64, 2048));
        creative.set(false);
        assertEquals(1, source.extract(64).getCount());
        assertTrue(source.extract(64).isEmpty());
    }

    @Test
    void fluidCopiesRequireASeedAndKeepFluidIdentity() {
        AtomicBoolean creative = new AtomicBoolean(true);
        PylonTank<String> source = new PylonTank<>(creative::get, () -> 32000);
        assertEquals(0, source.extract(1000, false));
        source.insert("water", 1, 32000, String::equals, false);
        assertEquals(32000, source.available());
        assertEquals(8000, source.extract(8000, true));
        assertEquals(8000, source.extract(8000, false));
        assertEquals(1, source.amount());
        PylonTank<String> target = new PylonTank<>();
        assertEquals(32000, source.moveTo(target, 64000, 64000, String::equals));
        assertEquals(32000, target.amount());
        assertEquals(1, source.amount());
        assertEquals(0, source.insert("lava", 1000, 32000, String::equals, false));
        creative.set(false);
        assertEquals(1, source.extract(8000, false));
        assertEquals(0, source.extract(8000, false));
    }

    @Test
    void energyCopiesKeepTransferLimitsAndTransactionSnapshots() {
        AtomicBoolean creative = new AtomicBoolean(true);
        PylonEnergy energy = new PylonEnergy(creative::get);
        assertEquals(0, energy.limit(false, 1000, 1_000_000, 1000, 1));
        energy.restore(new PylonEnergy.State(1, Long.MIN_VALUE, 0, 0));
        var before = energy.snapshot();
        assertEquals(1000, energy.limit(false, 1000, 1_000_000, 1000, 1));
        energy.transfer(false, 1000, 1);
        assertEquals(1, energy.amount());
        assertEquals(0, energy.limit(false, 1000, 1_000_000, 1000, 1));
        energy.restore(before);
        assertEquals(1000, energy.limit(false, 1000, 1_000_000, 1000, 1));
        creative.set(false);
        assertEquals(1, energy.limit(false, 1000, 1_000_000, 1000, 2));
        energy.transfer(false, 1, 2);
        assertEquals(0, energy.amount());
    }

    @Test
    void saveAndReloadKeepRealAmountsInsteadOfVirtualBalances(MinecraftServer server) {
        var state = SuperPylonsMod.SUPER_PYLON.get().defaultBlockState();
        var pylon = new SuperPylonBlockEntity(BlockPos.ZERO, state);
        String normal = "xp_tank_upgrade,water_generator_upgrade,energy_upgrade_6,storage_upgrade_6";
        pylon.loadUpgrades(normal + ",creative_storage_upgrade");
        pylon.contents.restore(new ItemStack(Items.DIAMOND), 1, pylon.getItemCapacity());
        pylon.loadSpecialtyAmounts(1, 1);
        pylon.generateFluid(Fluids.WATER, 1);
        var loaded = new SuperPylonBlockEntity(BlockPos.ZERO, state);
        PylonTestSerialization.load(loaded, pylon.getUpdateTag(server.registryAccess()), server);
        assertTrue(loaded.isCreativeStorage());
        assertEquals(Long.MAX_VALUE, loaded.getAvailableItemCount());
        assertEquals(Long.MAX_VALUE, loaded.getAvailableExperience());
        assertEquals(2_000_000_000, loaded.getAvailableEnergy());
        assertEquals(1, loaded.getStoredCount());
        assertEquals(1, loaded.getExperience());
        assertEquals(1, loaded.getEnergy());
        assertEquals(1, loaded.getFluidAmount());
        assertTrue(loaded.hasPackedDrop());
        loaded.loadUpgrades(normal);
        assertFalse(loaded.isCreativeStorage());
        assertEquals(1, loaded.getAvailableItemCount());
        assertEquals(1, loaded.getAvailableExperience());
        assertEquals(1, loaded.getAvailableEnergy());
        assertEquals(Items.COMMAND_BLOCK, SuperPylonsMod.UPGRADES.get(PylonUpgrade.CREATIVE_STORAGE).get());
    }
}
