package net.gasharaujo.superpylons.compat;

import java.util.function.Consumer;
import net.gasharaujo.superpylons.block.PylonBlockEntityBase;
import net.gasharaujo.superpylons.block.PylonUpgrade;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

final class PylonTooltip {
    private PylonTooltip() {}

    static void append(PylonBlockEntityBase pylon, Consumer<Component> lines) {
        lines.accept(storageLine(pylon));
        var upgrades = pylon.getInstalledUpgrades();
        if (!upgrades.isEmpty()) lines.accept(Component.translatable("tooltip.gash_super_pylons.upgrades"));
        for (PylonUpgrade upgrade : upgrades) {
            Component name = Component.translatable("item.gash_super_pylons." + upgrade.id()).withStyle(ChatFormatting.BLUE);
            Component info = switch (upgrade.slot()) {
                case SPEED -> Component.translatable("tooltip.gash_super_pylons.speed_info", name, upgrade.value());
                case QUANTITY -> Component.translatable("tooltip.gash_super_pylons.quantity_info", name, upgrade.value());
                case RANGE -> Component.translatable("tooltip.gash_super_pylons.range_info", name, upgrade.value() * 2 + 1);
                case STORAGE -> Component.translatable("tooltip.gash_super_pylons.capacity_info", name,
                    upgrade == PylonUpgrade.STORAGE_6 ? capacityLabel(Long.MAX_VALUE) : "×" + upgrade.value());
                default -> upgrade.isEnergy() ? Component.translatable("tooltip.gash_super_pylons.transfer_info",
                    name, upgrade.energyTransfer() == Integer.MAX_VALUE ? capacityLabel(Long.MAX_VALUE) : upgrade.energyTransfer()) : name;
            };
            lines.accept(info);
        }
        if (pylon.getProcessing() != null && pylon.getFuelUses() > 0) {
            double uses = pylon.getFuelUses();
            lines.accept(Component.translatable("tooltip.gash_super_pylons.fuel_uses",
                uses % 1 == 0 ? Long.toString((long) uses) : Double.toString(uses)));
        }
    }

    static Component storageLine(PylonBlockEntityBase pylon) {
        PylonUpgrade specialty = pylon.getSpecialty();
        if (specialty == PylonUpgrade.XP_TANK) {
            return withItems(pylon, Component.translatable("tooltip.gash_super_pylons.experience", pylon.getExperience()));
        }
        if (pylon.isEnergy()) {
            return withItems(pylon, Component.translatable("tooltip.gash_super_pylons.energy",
                pylon.getEnergy(), pylon.getEnergyCapacity()));
        }
        if (pylon.isTank()) {
            long amount = pylon.getFluidAmount();
            Object capacity = capacityLabel(pylon.getFluidCapacity());
            if (amount == 0) return Component.translatable("tooltip.gash_super_pylons.empty_tank", capacity);
            return Component.translatable("tooltip.gash_super_pylons.fluid", pylon.getFluidName(), amount, capacity);
        }
        if (specialty != null && specialty.locksItems()) {
            return Component.translatable("tooltip.gash_super_pylons.item_storage_locked");
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
