package net.gasharaujo.superpylons.block;

import static org.junit.jupiter.api.Assertions.*;

import net.gasharaujo.superpylons.SuperPylonsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(EphemeralTestServerProvider.class)
final class PylonContentsTest {
    @Test
    void capacityAndPartialRefillConserveItems(MinecraftServer server) {
        PylonContents storage = new PylonContents();
        for (int i = 0; i < 32; i++) {
            ItemStack incoming = new ItemStack(Items.IRON_INGOT, 64);
            assertEquals(64, storage.insert(incoming));
            assertTrue(incoming.isEmpty());
        }
        assertEquals(2048, storage.count());
        assertEquals(1, storage.displayItem().getCount());
        ItemStack overflow = new ItemStack(Items.IRON_INGOT, 64);
        assertEquals(0, storage.insert(overflow));
        assertEquals(64, overflow.getCount());
        assertEquals(1, storage.extract(1).getCount());
        assertEquals(1, storage.insert(overflow));
        assertEquals(63, overflow.getCount());
        int withdrawn = 0;
        while (storage.count() > 0) {
            ItemStack stack = storage.extract(Integer.MAX_VALUE);
            assertTrue(stack.getCount() <= stack.getMaxStackSize());
            withdrawn += stack.getCount();
        }
        assertEquals(2048, withdrawn);
        assertTrue(storage.displayItem().isEmpty());
    }

    @Test
    void nameDamageAndCustomDataMustMatch(MinecraftServer server) {
        PylonContents storage = new PylonContents();
        ItemStack original = new ItemStack(Items.DIAMOND_PICKAXE);
        original.set(DataComponents.CUSTOM_NAME, Component.literal("Exact core"));
        original.setDamageValue(7);
        CompoundTag tag = new CompoundTag();
        tag.putString("owner", "original");
        original.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        assertEquals(1, storage.insert(original.copy()));

        ItemStack different = original.copy();
        different.setDamageValue(8);
        assertEquals(0, storage.insert(different));
        assertEquals(1, different.getCount());
        different = original.copy();
        different.set(DataComponents.CUSTOM_NAME, Component.literal("Other name"));
        assertEquals(0, storage.insert(different));
        different = original.copy();
        CompoundTag differentTag = new CompoundTag();
        differentTag.putString("owner", "other");
        different.set(DataComponents.CUSTOM_DATA, CustomData.of(differentTag));
        assertEquals(0, storage.insert(different));
        assertEquals(0, storage.insert(new ItemStack(Items.IRON_INGOT)));
        ItemStack enchanted = original.copy();
        enchanted.enchant(server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(Enchantments.UNBREAKING), 1);
        assertEquals(0, storage.insert(enchanted));
        assertEquals(1, storage.insert(original.copy()));
        assertEquals(2, storage.count());
        // Unstackable items must still be returned as legal one-item stacks.
        assertEquals(1, storage.extract(Integer.MAX_VALUE).getCount());
    }

    @Test
    void modifierTagIsExcludedWithoutConsumingItems(MinecraftServer server) {
        ItemStack modifier = new ItemStack(Items.STICK, 64);
        assertTrue(PylonItemRules.isModifier(modifier), "The test-only modifier tag must be loaded");
        PylonContents storage = new PylonContents();
        assertEquals(0, storage.insert(modifier));
        assertEquals(64, modifier.getCount());
        assertEquals(0, storage.count());
    }

    @Test
    void emptyStorageCanChangeTypeAndDisplayIsDefensive(MinecraftServer server) {
        PylonContents storage = new PylonContents();
        assertEquals(1, storage.insert(new ItemStack(Items.DIAMOND)));
        storage.displayItem().setCount(0);
        assertEquals(1, storage.count());
        assertEquals(1, storage.extract(1).getCount());
        assertEquals(16, storage.insert(new ItemStack(Items.ENDER_PEARL, 16)));
        assertEquals(16, storage.extract(Integer.MAX_VALUE).getCount());
        assertTrue(storage.displayItem().isEmpty());
        assertTrue(storage.extract(1).isEmpty());
    }

    @Test
    void blockEntityNbtAndClientUpdateTagPreserveTemplateAndCount(MinecraftServer server) {
        var state = SuperPylonsMod.SUPER_PYLON.get().defaultBlockState();
        SuperPylonBlockEntity original = new SuperPylonBlockEntity(BlockPos.ZERO, state);
        ItemStack template = new ItemStack(Items.DIAMOND);
        template.set(DataComponents.CUSTOM_NAME, Component.literal("Stored name"));
        original.contents.restore(template, 2048);
        CompoundTag saved = original.getUpdateTag(server.registryAccess());
        SuperPylonBlockEntity loaded = new SuperPylonBlockEntity(BlockPos.ZERO, state);
        PylonTestSerialization.load(loaded, saved, server);
        assertEquals(2048, loaded.getStoredCount());
        assertEquals(1, loaded.getStoredItem().getCount());
        assertTrue(PylonItemRules.exactlyEqual(template, loaded.getStoredItem()));
    }

    @Test
    void invalidCountsNormalizeAndIronMiningPropertiesArePreserved(MinecraftServer server) {
        PylonContents storage = new PylonContents();
        storage.restore(new ItemStack(Items.DIAMOND), Integer.MAX_VALUE);
        assertEquals(2048, storage.count());
        storage.restore(new ItemStack(Items.DIAMOND), -1);
        assertEquals(0, storage.count());
        storage.restore(ItemStack.EMPTY, 128);
        assertEquals(0, storage.count());
        var state = SuperPylonsMod.SUPER_PYLON.get().defaultBlockState();
        assertEquals(Blocks.IRON_BLOCK.defaultBlockState().getDestroySpeed(null, BlockPos.ZERO),
            state.getDestroySpeed(null, BlockPos.ZERO));
        assertTrue(state.requiresCorrectToolForDrops());
        assertTrue(SuperPylonsMod.SUPER_PYLON.get().getExplosionResistance() >= 3600000.0F);
        // Even a direct explosion callback must not touch the level.
        SuperPylonsMod.SUPER_PYLON.get().onBlockExploded(state, null, BlockPos.ZERO, null);
        SuperPylonsMod.SUPER_PYLON.get().onExplosionHit(state, null, BlockPos.ZERO, null,
            (item, pos) -> fail("An explosion must not generate drops from a surviving pylon"));
    }
}
