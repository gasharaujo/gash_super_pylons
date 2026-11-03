package net.gasharaujo.superpylons.compat;

import net.gasharaujo.superpylons.block.PylonBlockEntityBase;
import net.minecraft.network.chat.Component;

final class PylonTooltip {
    private PylonTooltip() {}

    static Component storageLine(PylonBlockEntityBase pylon) {
        if (pylon.isTank()) {
            long amount = pylon.getFluidAmount();
            Object capacity = capacityLabel(pylon.getFluidCapacity());
            if (amount == 0) return Component.translatable("tooltip.gash_super_pylons.empty_tank", capacity);
            return Component.translatable("tooltip.gash_super_pylons.fluid", pylon.getFluidName(), amount, capacity);
        }
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
