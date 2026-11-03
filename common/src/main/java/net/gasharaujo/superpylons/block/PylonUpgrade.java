package net.gasharaujo.superpylons.block;

import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public enum PylonUpgrade {
    STORAGE_1("storage_upgrade_1", Slot.STORAGE, 1, 2, null, null),
    STORAGE_2("storage_upgrade_2", Slot.STORAGE, 2, 4, null, null),
    STORAGE_3("storage_upgrade_3", Slot.STORAGE, 3, 8, null, null),
    STORAGE_4("storage_upgrade_4", Slot.STORAGE, 4, 16, null, null),
    STORAGE_5("storage_upgrade_5", Slot.STORAGE, 5, 32, null, null),
    STORAGE_6("storage_upgrade_6", Slot.STORAGE, 6, 0, null, null),
    SPEED_1("speed_upgrade_1", Slot.SPEED, 1, 40, null, null),
    SPEED_2("speed_upgrade_2", Slot.SPEED, 2, 30, null, null),
    SPEED_3("speed_upgrade_3", Slot.SPEED, 3, 20, null, null),
    SPEED_4("speed_upgrade_4", Slot.SPEED, 4, 10, null, null),
    SPEED_5("speed_upgrade_5", Slot.SPEED, 5, 5, null, null),
    SPEED_6("speed_upgrade_6", Slot.SPEED, 6, 1, null, null),
    QUANTITY_1("quantity_upgrade_1", Slot.QUANTITY, 1, 1, null, null),
    QUANTITY_2("quantity_upgrade_2", Slot.QUANTITY, 2, 4, null, null),
    QUANTITY_3("quantity_upgrade_3", Slot.QUANTITY, 3, 8, null, null),
    QUANTITY_4("quantity_upgrade_4", Slot.QUANTITY, 4, 16, null, null),
    QUANTITY_5("quantity_upgrade_5", Slot.QUANTITY, 5, 32, null, null),
    QUANTITY_6("quantity_upgrade_6", Slot.QUANTITY, 6, 64, null, null),
    COBBLESTONE_GENERATOR("cobblestone_generator_upgrade", Slot.SPECIALTY, 1, 0, () -> Items.COBBLESTONE, null),
    DEEPSLATE_GENERATOR("deepslate_generator_upgrade", Slot.SPECIALTY, 1, 0, () -> Items.DEEPSLATE, null),
    BLACKSTONE_GENERATOR("blackstone_generator_upgrade", Slot.SPECIALTY, 1, 0, () -> Items.BLACKSTONE, null),
    GLASS_GENERATOR("glass_generator_upgrade", Slot.SPECIALTY, 1, 0, () -> Items.GLASS, null),
    NETHERRACK_GENERATOR("netherrack_generator_upgrade", Slot.SPECIALTY, 1, 0, () -> Items.NETHERRACK, null),
    SAND_GENERATOR("sand_generator_upgrade", Slot.SPECIALTY, 1, 0, () -> Items.SAND, null),
    FLUID_STORAGE("fluid_storage_upgrade", Slot.SPECIALTY, 1, 0, null, null),
    WATER_GENERATOR("water_generator_upgrade", Slot.SPECIALTY, 1, 0, null, () -> Fluids.WATER),
    LAVA_GENERATOR("lava_generator_upgrade", Slot.SPECIALTY, 1, 0, null, () -> Fluids.LAVA),
    ANVIL("anvil_upgrade", Slot.SPECIALTY, 1, 0, null, null),
    ENCHANTING("enchanting_upgrade", Slot.SPECIALTY, 1, 0, null, null),
    CLICKER("clicker_upgrade", Slot.SPECIALTY, 1, 0, null, null);

    // Iteration order is also the order used for removal.
    public enum Slot { SPEED, QUANTITY, SPECIALTY, STORAGE }

    private final String id;
    private final Slot slot;
    private final int tier;
    private final int value;
    private final Supplier<Item> item;
    private final Supplier<Fluid> fluid;

    PylonUpgrade(String id, Slot slot, int tier, int value, Supplier<Item> item, Supplier<Fluid> fluid) {
        this.id = id;
        this.slot = slot;
        this.tier = tier;
        this.value = value;
        this.item = item;
        this.fluid = fluid;
    }

    public String id() { return id; }
    public Slot slot() { return slot; }
    public int tier() { return tier; }
    public int value() { return value; }
    public Item generatedItem() { return item == null ? Items.AIR : item.get(); }
    public Fluid generatedFluid() { return fluid == null ? Fluids.EMPTY : fluid.get(); }
    public boolean generatesFluid() { return fluid != null; }
    public boolean isTank() { return this == FLUID_STORAGE || generatesFluid(); }
    public boolean locksItems() { return isTank() || this == ANVIL || this == ENCHANTING; }

    public static PylonUpgrade byId(String id) {
        for (PylonUpgrade upgrade : values()) {
            if (upgrade.id.equals(id)) return upgrade;
        }
        return null;
    }

    public static long itemCapacity(PylonUpgrade storage) {
        if (storage == STORAGE_6) return Long.MAX_VALUE;
        return (long) PylonContents.CAPACITY * (storage == null ? 1 : storage.value);
    }
}
