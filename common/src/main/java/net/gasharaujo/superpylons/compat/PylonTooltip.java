package net.gasharaujo.superpylons.compat;

import net.gasharaujo.superpylons.block.PylonBlockEntityBase;
import net.gasharaujo.superpylons.block.PylonContents;
import net.minecraft.network.chat.Component;

final class PylonTooltip {
    private PylonTooltip() {}

    static Component storageLine(PylonBlockEntityBase pylon) {
        int count = pylon.getStoredCount();
        if (count == 0) {
            return Component.translatable("tooltip.gash_super_pylons.empty", PylonContents.CAPACITY);
        }
        return Component.translatable("tooltip.gash_super_pylons.storage",
            pylon.getStoredItem().getHoverName(), count, PylonContents.CAPACITY);
    }
}
