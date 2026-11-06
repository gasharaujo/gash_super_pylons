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
        var tool = pylon.getBreakTool();
        var toolLine = tool.isEmpty() ? null : Component.translatable(
            pylon.getSpecialty() == PylonUpgrade.HARVESTER ? "tooltip.gash_super_pylons.weapon" :
                "tooltip.gash_super_pylons.break_tool", tool.getHoverName());
        if (toolLine != null && !pylon.hasSuperUpgrade()) lines.accept(toolLine);
        var furnace = pylon.getFurnaceAttachment();
        for (PylonUpgrade upgrade : upgrades) {
            Component name = Component.translatable("item.gash_super_pylons." + upgrade.id()).withStyle(ChatFormatting.BLUE);
            Component info = switch (upgrade.slot()) {
                case SPEED -> Component.translatable("tooltip.gash_super_pylons.speed_info", name, upgrade.value());
                case QUANTITY -> pylon.hasSuperUpgrade() && pylon.getSpecialty() == PylonUpgrade.HARVESTER ?
                    Component.translatable("tooltip.gash_super_pylons.target_quantity", name, upgrade.tier() + 1) :
                    Component.translatable("tooltip.gash_super_pylons.quantity_info", name, upgrade.value());
                case RANGE -> rangeInfo(pylon, upgrade, name);
                case STORAGE -> Component.translatable("tooltip.gash_super_pylons.capacity_info", name,
                    upgrade == PylonUpgrade.STORAGE_6 ? capacityLabel(Long.MAX_VALUE) : "×" + upgrade.value());
                case SPECIALTY -> upgrade == PylonUpgrade.DROPPER ?
                    Component.translatable("tooltip.gash_super_pylons.dropper_distance", name, pylon.dropDistance()) : name;
                case ENERGY -> Component.translatable("tooltip.gash_super_pylons.transfer_info",
                    name, upgrade.energyTransfer() == Integer.MAX_VALUE ? capacityLabel(Long.MAX_VALUE) : upgrade.energyTransfer());
                default -> name;
            };
            if (upgrade == PylonUpgrade.SMELT && !furnace.isEmpty()) {
                lines.accept(Component.translatable("tooltip.gash_super_pylons.furnace_attachment", furnace.getHoverName()));
            }
            lines.accept(info);
            if (upgrade == PylonUpgrade.SUPER && toolLine != null) lines.accept(toolLine);
        }
        if (pylon.getProcessing() != null && pylon.getFuelUses() > 0) {
            double uses = pylon.getFuelUses();
            lines.accept(Component.translatable("tooltip.gash_super_pylons.fuel_uses",
                uses % 1 == 0 ? Long.toString((long) uses) : Double.toString(uses)));
        }
        if (pylon.isWorkshop()) {
            lines.accept(Component.translatable("tooltip.gash_super_pylons.workshop." + pylon.getWorkshopStatus()));
            if (pylon.getWorkshopCost() > 0) lines.accept(Component.translatable("tooltip.gash_super_pylons.workshop_cost", pylon.getWorkshopCost()));
            if (pylon.getSpecialty() == PylonUpgrade.ENCHANTING) {
                lines.accept(Component.translatable("tooltip.gash_super_pylons.enchanting_power",
                    pylon.getEnchantmentPower(), pylon.getMinimumEnchantments()));
                if (!pylon.hasSuperUpgrade()) {
                    lines.accept(Component.translatable("tooltip.gash_super_pylons.enchanting_quantity_requires_super"));
                }
                if (!pylon.getShelfItems().isEmpty()) {
                    lines.accept(Component.translatable("tooltip.gash_super_pylons.shelves", pylon.getShelfItems().size()));
                    java.util.Map<net.minecraft.world.item.Item, Integer> counts = new java.util.LinkedHashMap<>();
                    for (var shelf : pylon.getShelfItems()) counts.merge(shelf.getItem(), 1, Integer::sum);
                    counts.forEach((item, count) -> lines.accept(Component.translatable("tooltip.gash_super_pylons.shelf", count, new net.minecraft.world.item.ItemStack(item).getHoverName())));
                }
            }
        }
    }

    private static Component rangeInfo(PylonBlockEntityBase pylon, PylonUpgrade upgrade, Component name) {
        int width = upgrade.value() * 2 + 1;
        var specialty = pylon.getSpecialty();
        if (pylon.hasSuperUpgrade()) {
            if (specialty == PylonUpgrade.BREAK || specialty == PylonUpgrade.FLUID_STORAGE) {
                return Component.translatable("tooltip.gash_super_pylons.chunk_range", name, upgrade.tier() * 2 + 1);
            }
            if (specialty == PylonUpgrade.PLACER) {
                return Component.translatable("tooltip.gash_super_pylons.plane_range", name, width);
            }
        }
        if (specialty == PylonUpgrade.DROPPER) {
            return Component.translatable("tooltip.gash_super_pylons.dropper_distance", name, pylon.dropDistance());
        }
        return Component.translatable("tooltip.gash_super_pylons.range_info", name, width);
    }

    static Component storageLine(PylonBlockEntityBase pylon) {
        Component storage = primaryStorage(pylon);
        if (!pylon.isEnergy() || pylon.isVoid()) return storage;
        return Component.empty().append(storage).append(" | ").append(Component.translatable("tooltip.gash_super_pylons.energy",
            amountLabel(pylon, pylon.getAvailableEnergy()), pylon.getEnergyCapacity()));
    }

    private static Component primaryStorage(PylonBlockEntityBase pylon) {
        PylonUpgrade specialty = pylon.getSpecialty();
        if (pylon.isVoid()) return Component.translatable("tooltip.gash_super_pylons.void");
        Component modes = Component.empty();
        boolean hasMode = false;
        if (specialty == PylonUpgrade.XP_TANK) {
            modes = Component.translatable("tooltip.gash_super_pylons.experience",
                amountLabel(pylon, pylon.getAvailableExperience()), capacityLabel(pylon.getExperienceCapacity()));
            hasMode = true;
        }
        if (pylon.isTank()) {
            long amount = pylon.getFluidAmount();
            Object capacity = capacityLabel(pylon.getFluidCapacity());
            Component fluid = amount == 0 ? Component.translatable("tooltip.gash_super_pylons.empty_tank", capacity) :
                Component.translatable("tooltip.gash_super_pylons.fluid", pylon.getFluidName(), amountLabel(pylon, amount), capacity);
            modes = hasMode ? Component.empty().append(modes).append(" | ").append(fluid) : fluid;
            hasMode = true;
        }
        if (hasMode) return withItems(pylon, modes);
        if (specialty != null && specialty.locksItems()) {
            return withItems(pylon, Component.translatable("tooltip.gash_super_pylons.item_storage_locked"));
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
            pylon.getStoredItem().getHoverName(), amountLabel(pylon, count), capacity);
    }

    private static Object amountLabel(PylonBlockEntityBase pylon, long amount) {
        return pylon.isCreativeStorage() && amount > 0 ? Component.literal("∞") : amount;
    }

    private static Object capacityLabel(long capacity) {
        return capacity == Long.MAX_VALUE ? Component.translatable("tooltip.gash_super_pylons.unlimited") : capacity;
    }
}
