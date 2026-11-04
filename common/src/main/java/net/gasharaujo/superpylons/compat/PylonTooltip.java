package net.gasharaujo.superpylons.compat;

import net.gasharaujo.superpylons.block.PylonBlockEntityBase;
import net.gasharaujo.superpylons.block.PylonUpgrade;
import net.minecraft.network.chat.Component;

final class PylonTooltip {
    private PylonTooltip() {}

    static Component storageLine(PylonBlockEntityBase pylon) {
        PylonUpgrade specialty = pylon.getSpecialty();
        if (specialty == PylonUpgrade.XP_TANK) {
            return withItems(pylon, Component.translatable("tooltip.gash_super_pylons.experience", pylon.getExperience()));
        }
        if (pylon.isEnergy()) {
            return withItems(pylon, Component.translatable("tooltip.gash_super_pylons.energy",
                pylon.getEnergy(), pylon.getEnergyCapacity()));
        }
        if (specialty == PylonUpgrade.FERTILIZER || specialty == PylonUpgrade.HARVESTER) {
            return Component.translatable("item.gash_super_pylons." + specialty.id());
        }
        if (pylon.isTank()) {
            long amount = pylon.getFluidAmount();
            Object capacity = capacityLabel(pylon.getFluidCapacity());
            if (amount == 0) return Component.translatable("tooltip.gash_super_pylons.empty_tank", capacity);
            return Component.translatable("tooltip.gash_super_pylons.fluid", pylon.getFluidName(), amount, capacity);
        }
        return itemStorageLine(pylon);
    }

    private static Component withItems(PylonBlockEntityBase pylon, Component mode) {
        if (pylon.getStoredCount() == 0) return mode;
        return Component.empty().append(mode).append(" | ").append(itemStorageLine(pylon));
    }

    private static Component itemStorageLine(PylonBlockEntityBase pylon) {
        long count = pylon.getStoredCount();
        Object capacity = capacityLabel(pylon.getItemCapacity());
        if (count == 0) {
            return Component.translatable("tooltip.gash_super_pylons.empty", capacity);
        }
        return Component.translatable("tooltip.gash_super_pylons.storage",
            pylon.getStoredItem().getHoverName(), count, capacity);
    }

    private static Object capacityLabel(long capacity) {
        return capacity == Long.MAX_VALUE ? Component.translatable("tooltip.gash_super_pylons.unlimited") : capacity;
    }
}
