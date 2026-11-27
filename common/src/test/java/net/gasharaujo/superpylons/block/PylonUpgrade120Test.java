package net.gasharaujo.superpylons.block;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.authlib.GameProfile;
import java.io.IOException;
import java.util.UUID;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(EphemeralTestServerProvider.class)
final class PylonUpgrade120Test {
    private static final GameProfile PLAYER = new GameProfile(
        UUID.fromString("574fca67-909b-48f9-9804-c94866ca9257"), "PylonTest");

    @Test
    void redstoneReportsFullnessAndPausesWithoutChangingStoredItems(MinecraftServer server) {
        server.executeBlocking(() -> {
            var level = testLevel(server);
            var pos = new BlockPos(0, 128, 0);
            var pylon = place(level, pos, false, "redstone_level_upgrade,redstone_upgrade,cobblestone_generator_upgrade,speed_upgrade_6");
            pylon.insertFrom(new ItemStack(Items.COBBLESTONE, 1024));
            pylon.tick(level);
            assertEquals(8, pylon.redstoneSignal(Direction.DOWN));
            assertEquals(0, pylon.redstoneSignal(Direction.NORTH));
            assertEquals(8, level.getSignal(pos, Direction.UP));

            level.setBlock(pos.north(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            long beforePause = pylon.getStoredCount();
            pylon.tick(level);
            assertEquals(0, pylon.redstoneSignal(Direction.DOWN));
            assertEquals(beforePause, pylon.getStoredCount());
            assertEquals(2048, pylon.getItemCapacity());

            level.setBlock(pos.north(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            pylon.tick(level);
            assertEquals(beforePause + 1, pylon.getStoredCount());
        });
    }

    @Test
    void hubLinksByRangeAndPersistsOnlyItsConfiguration(MinecraftServer server) {
        server.executeBlocking(() -> {
            var level = testLevel(server);
            var pos = new BlockPos(64, 128, 64);
            var hub = place(level, pos, true, "energy_upgrade_6");
            var tank = place(level, pos.east(4), false, "xp_tank_upgrade");
            var item = place(level, pos.east(18), false, "storage_upgrade_6");
            var player = hub.getAutomationPlayer(level, PLAYER);
            hub.linkAuxiliary(tank, player);
            hub.linkAuxiliary(item, player);
            assertEquals(1, hub.hub().size());
            assertFalse(hub.isEnergy());

            hub.loadUpgrades("range_upgrade_1");
            hub.linkAuxiliary(item, player);
            assertSame(item, hub.hub().target(1));
            assertEquals(12, hub.receiveExperience(12, true));
            assertEquals(0, tank.getExperience());
            assertEquals(12, hub.receiveExperience(12, false));
            assertEquals(12, tank.getExperience());

            long bulk = (long) Integer.MAX_VALUE + 8;
            var stock = new ItemStack(Items.IRON_INGOT);
            assertEquals(bulk, item.insertItemAmount(stock, bulk, false));
            assertEquals(bulk, item.extractItemAmount(stock, bulk, true));
            assertEquals(bulk, item.getStoredCount());
            assertEquals(1, item.extractItemAmount(stock, 1, false));

            var water = place(level, pos.south(4), false, "fluid_storage_upgrade");
            var battery = place(level, pos.south(6), false, "energy_upgrade_1");
            hub.linkAuxiliary(water, player);
            hub.linkAuxiliary(battery, player);
            PylonHubTestPorts.transfer(level, hub.getBlockPos(), water, battery);

            var saved = hub.getUpdateTag(server.registryAccess());
            assertTrue(saved.contains("hub_links"));
            for (String field : new String[] {"stored_item", "stored_count", "fluid_amount", "experience", "energy"}) {
                assertFalse(saved.contains(field), field);
            }
            var loaded = new SuperPylonBlockEntity(pos, SuperPylonsMod.PYLON_HUB.get().defaultBlockState());
            loaded.setLevel(level);
            PylonTestSerialization.load(loaded, saved, server);
            assertSame(item, loaded.hub().target(1));
            assertEquals(12, loaded.getExperience());
            assertEquals(bulk - 1, item.getStoredCount());
            loaded.loadUpgrades("");
            assertNull(loaded.hub().target(1));
        });
    }

    @Test
    void workshopsCopyBooksAndMoveEnchantmentsOrExperience(MinecraftServer server) {
        server.executeBlocking(() -> {
            var level = testLevel(server);
            var pos = new BlockPos(128, 128, 128);
            var main = place(level, pos, false, "grindstone_upgrade,speed_upgrade_6");
            var xp = place(level, pos.east(2), false, "xp_tank_upgrade");
            var supplies = place(level, pos.west(2), false, "");
            var player = main.getAutomationPlayer(level, PLAYER);
            main.linkAuxiliary(xp, player);
            main.linkAuxiliary(supplies, player);

            var registry = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var unbreaking = registry.getOrThrow(Enchantments.UNBREAKING);
            var mending = registry.getOrThrow(Enchantments.MENDING);
            var curse = registry.getOrThrow(Enchantments.VANISHING_CURSE);
            var pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
            pickaxe.enchant(unbreaking, 3);
            pickaxe.enchant(mending, 1);
            pickaxe.enchant(curse, 1);
            main.insertFrom(pickaxe.copy());
            main.tick(level);
            var ordinary = EnchantmentHelper.getEnchantmentsForCrafting(main.getStoredItem());
            assertEquals(1, ordinary.size());
            assertEquals(1, ordinary.getLevel(curse));
            assertTrue(xp.getExperience() > 0);

            long earned = xp.getExperience();
            main.contents.extract(1);
            main.loadUpgrades("grindstone_upgrade,speed_upgrade_6,super_upgrade,quantity_upgrade_2");
            main.insertFrom(pickaxe.copy());
            assertFalse(SuperPylonsMod.GRINDSTONE_CONSUMES_BOOKS.get());
            main.tick(level);
            assertEquals(earned, xp.getExperience());
            assertFalse(main.getStoredItem().isEnchanted());
            assertEquals(1, main.outputBuffer.size());
            var donor = main.outputBuffer.getFirst();
            assertTrue(donor.is(Items.ENCHANTED_BOOK));
            assertEquals(3, EnchantmentHelper.getEnchantmentsForCrafting(donor).size());

            main.contents.extract(1);
            main.outputBuffer.clear();
            SuperPylonsMod.GRINDSTONE_CONSUMES_BOOKS.set(true);
            try {
                main.insertFrom(pickaxe.copy());
                main.tick(level);
                assertEquals("books", main.getWorkshopStatus());
                supplies.insertFrom(new ItemStack(Items.BOOK));
                main.tick(level);
                assertEquals(0, supplies.getStoredCount());
                assertEquals(1, main.outputBuffer.size());
                assertEquals(earned, xp.getExperience());
            } finally {
                SuperPylonsMod.GRINDSTONE_CONSUMES_BOOKS.set(false);
            }

            main.contents.extract(1);
            main.outputBuffer.clear();
            main.loadUpgrades("anvil_upgrade,super_upgrade,speed_upgrade_6");
            xp.receiveExperience(30_000, false);
            long beforeCopy = xp.getExperience();
            supplies.insertFrom(donor.copy());
            main.insertFrom(new ItemStack(Items.BOOK));
            main.tick(level);
            assertTrue(PylonItemRules.exactlyEqual(donor, main.getStoredItem()));
            assertEquals(1, supplies.getStoredCount());
            assertTrue(xp.getExperience() < beforeCopy);
        });
    }

    @Test
    void removalSkipsBlockedUpgradesAndReturnsTheFirstAvailableItem(MinecraftServer server) {
        server.executeBlocking(() -> {
            var level = testLevel(server);
            var pos = new BlockPos(192, 128, 192);
            var pylon = place(level, pos, false, "energy_upgrade_1,smelt_upgrade,storage_upgrade_1");
            var player = pylon.getAutomationPlayer(level, new GameProfile(UUID.randomUUID(), "RemovalTest"));
            player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 1.5);
            player.setShiftKeyDown(true);
            pylon.loadSpecialtyAmounts(0, 500);
            pylon.loadFurnaceAttachment(new ItemStack(Items.SMOKER));
            pylon.outputBuffer.add(new ItemStack(Items.STONE));

            assertTrue(pylon.use(player, InteractionHand.MAIN_HAND));
            assertTrue(player.getInventory().contains(new ItemStack(Items.SMOKER)));
            assertTrue(pylon.getFurnaceAttachment().isEmpty());
            assertEquals(PylonUpgrade.SMELT, pylon.getProcessing());

            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            assertTrue(pylon.use(player, InteractionHand.MAIN_HAND));
            assertEquals(PylonContents.CAPACITY, pylon.getItemCapacity());
            assertTrue(player.getInventory().contains(new ItemStack(SuperPylonsMod.UPGRADES.get(PylonUpgrade.STORAGE_1).get())));
            assertEquals(500, pylon.getEnergy());
            assertEquals(PylonUpgrade.SMELT, pylon.getProcessing());
            assertEquals(1, pylon.outputBuffer.size());
            assertTrue(pylon.outputBuffer.getFirst().is(Items.STONE));

            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            assertTrue(pylon.use(player, InteractionHand.MAIN_HAND));
            assertEquals(2, pylon.getInstalledUpgrades().size());
            assertEquals(500, pylon.getEnergy());
            assertEquals(1, pylon.outputBuffer.size());

            pylon.outputBuffer.clear();
            assertTrue(pylon.use(player, InteractionHand.MAIN_HAND));
            assertNull(pylon.getProcessing());
            assertTrue(player.getInventory().contains(new ItemStack(SuperPylonsMod.UPGRADES.get(PylonUpgrade.SMELT).get())));
            assertEquals(500, pylon.getEnergy());
        });
    }

    @AfterAll
    static void closeTestWorld() {
        MinecraftServer server = EphemeralTestServerProvider.SERVER.get();
        if (server == null || server.overworld() == null) return;
        server.executeBlocking(() -> {
            var worlds = server.getAllLevels().iterator();
            while (worlds.hasNext()) {
                try {
                    worlds.next().close();
                } catch (IOException e) {
                    throw new IllegalStateException("Could not close the ephemeral test world", e);
                }
                worlds.remove();
            }
        });
    }

    private static ServerLevel testLevel(MinecraftServer server) {
        if (server.overworld() == null) {
            // The NeoForge JUnit server loads registries but leaves world creation to the test.
            try {
                var load = MinecraftServer.class.getDeclaredMethod("loadLevel");
                load.setAccessible(true);
                load.invoke(server);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Could not initialize the ephemeral test world", e);
            }
        }
        return server.overworld();
    }

    private static SuperPylonBlockEntity place(ServerLevel level, BlockPos pos, boolean hub, String upgrades) {
        var block = hub ? SuperPylonsMod.PYLON_HUB.get() : SuperPylonsMod.SUPER_PYLON.get();
        level.getChunkAt(pos);
        level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
        var pylon = assertInstanceOf(SuperPylonBlockEntity.class, level.getBlockEntity(pos));
        pylon.loadUpgrades(upgrades);
        return pylon;
    }
}
