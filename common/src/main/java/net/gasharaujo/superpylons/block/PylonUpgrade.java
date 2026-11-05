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
    QUANTITY_1("quantity_upgrade_1", Slot.QUANTITY, 1, 2, null, null),
    QUANTITY_2("quantity_upgrade_2", Slot.QUANTITY, 2, 4, null, null),
    QUANTITY_3("quantity_upgrade_3", Slot.QUANTITY, 3, 8, null, null),
    QUANTITY_4("quantity_upgrade_4", Slot.QUANTITY, 4, 16, null, null),
    QUANTITY_5("quantity_upgrade_5", Slot.QUANTITY, 5, 32, null, null),
    QUANTITY_6("quantity_upgrade_6", Slot.QUANTITY, 6, 64, null, null),
    RANGE_1("range_upgrade_1", Slot.RANGE, 1, 3, null, null),
    RANGE_2("range_upgrade_2", Slot.RANGE, 2, 4, null, null),
    RANGE_3("range_upgrade_3", Slot.RANGE, 3, 5, null, null),
    RANGE_4("range_upgrade_4", Slot.RANGE, 4, 6, null, null),
    RANGE_5("range_upgrade_5", Slot.RANGE, 5, 7, null, null),
    RANGE_6("range_upgrade_6", Slot.RANGE, 6, 8, null, null),
    COBBLESTONE_GENERATOR("cobblestone_generator_upgrade", Slot.GENERATOR, 1, 0, () -> Items.COBBLESTONE, null),
    DEEPSLATE_GENERATOR("deepslate_generator_upgrade", Slot.GENERATOR, 1, 0, () -> Items.DEEPSLATE, null),
    BLACKSTONE_GENERATOR("blackstone_generator_upgrade", Slot.GENERATOR, 1, 0, () -> Items.BLACKSTONE, null),
    GLASS_GENERATOR("glass_generator_upgrade", Slot.GENERATOR, 1, 0, () -> Items.GLASS, null),
    NETHERRACK_GENERATOR("netherrack_generator_upgrade", Slot.GENERATOR, 1, 0, () -> Items.NETHERRACK, null),
    SAND_GENERATOR("sand_generator_upgrade", Slot.GENERATOR, 1, 0, () -> Items.SAND, null),
    FLUID_STORAGE("fluid_storage_upgrade", Slot.SPECIALTY, 1, 0, null, null),
    WATER_GENERATOR("water_generator_upgrade", Slot.GENERATOR, 1, 0, null, () -> Fluids.WATER),
    LAVA_GENERATOR("lava_generator_upgrade", Slot.GENERATOR, 1, 0, null, () -> Fluids.LAVA),
    ANVIL("anvil_upgrade", Slot.SPECIALTY, 1, 0, null, null),
    ENCHANTING("enchanting_upgrade", Slot.SPECIALTY, 1, 0, null, null),
    CLICKER("clicker_upgrade", Slot.SPECIALTY, 1, 0, null, null),
    XP_TANK("xp_tank_upgrade", Slot.SPECIALTY, 1, 0, null, null),
    ENERGY_1("energy_upgrade_1", Slot.ENERGY, 1, 1_000_000, null, null),
    ENERGY_2("energy_upgrade_2", Slot.ENERGY, 2, 10_000_000, null, null),
    ENERGY_3("energy_upgrade_3", Slot.ENERGY, 3, 40_000_000, null, null),
    ENERGY_4("energy_upgrade_4", Slot.ENERGY, 4, 100_000_000, null, null),
    ENERGY_5("energy_upgrade_5", Slot.ENERGY, 5, 400_000_000, null, null),
    ENERGY_6("energy_upgrade_6", Slot.ENERGY, 6, 2_000_000_000, null, null),
    FERTILIZER("fertilizer_upgrade", Slot.SPECIALTY, 1, 0, null, null),
    HARVESTER("harvester_upgrade", Slot.SPECIALTY, 1, 0, null, null),
    BREAK("break_upgrade", Slot.SPECIALTY, 1, 0, null, null),
    FILTER("filter_upgrade", Slot.FILTER, 1, 0, null, null),
    MAGNET("magnet_upgrade", Slot.SPECIALTY, 1, 0, null, null),
    PLANTER("planter_upgrade", Slot.SPECIALTY, 1, 0, null, null),
    PLACER("placer_upgrade", Slot.SPECIALTY, 1, 0, null, null),
    DROPPER("dropper_upgrade", Slot.SPECIALTY, 1, 0, null, null),
    VOID("void_upgrade", Slot.SPECIALTY, 1, 0, null, null),
    CHUNK_LOADER("chunk_loader_upgrade", Slot.CHUNK_LOADER, 1, 0, null, null),
    SUPER("super_upgrade", Slot.SUPER, 1, 0, null, null),
    CREATIVE_STORAGE("creative_storage_upgrade", Slot.CREATIVE, 1, 0, null, null),
    SMELT("smelt_upgrade", Slot.PROCESSING, 1, 0, null, null),
    PULVERIZE("pulverize_upgrade", Slot.PROCESSING, 1, 0, null, null),
    INSERT_ITEM("insert_item_upgrade", Slot.ITEM_TRANSFER, PylonWire.Type.ITEM, true),
    EXTRACT_ITEM("extract_item_upgrade", Slot.ITEM_TRANSFER, PylonWire.Type.ITEM, false),
    INSERT_FLUID("insert_fluid_upgrade", Slot.FLUID_TRANSFER, PylonWire.Type.FLUID, true),
    EXTRACT_FLUID("extract_fluid_upgrade", Slot.FLUID_TRANSFER, PylonWire.Type.FLUID, false),
    INSERT_ENERGY("insert_energy_upgrade", Slot.ENERGY_TRANSFER, PylonWire.Type.ENERGY, true),
    EXTRACT_ENERGY("extract_energy_upgrade", Slot.ENERGY_TRANSFER, PylonWire.Type.ENERGY, false),
    INSERT_XP("insert_xp_upgrade", Slot.XP_TRANSFER, PylonWire.Type.XP, true),
    EXTRACT_XP("extract_xp_upgrade", Slot.XP_TRANSFER, PylonWire.Type.XP, false);

    // Default removal order. Generators are removed before processing.
    public enum Slot {
        SUPER, CREATIVE, FILTER, SPEED, QUANTITY, RANGE, ENERGY, ITEM_TRANSFER, FLUID_TRANSFER, ENERGY_TRANSFER, XP_TRANSFER,
        CHUNK_LOADER, GENERATOR, PROCESSING, SPECIALTY, STORAGE
    }

    private final String id;
    private final Slot slot;
    private final int tier;
    private final int value;
    private final Supplier<Item> item;
    private final Supplier<Fluid> fluid;
    private final PylonWire.Type transferType;
    private final boolean insert;

    PylonUpgrade(String id, Slot slot, int tier, int value, Supplier<Item> item, Supplier<Fluid> fluid) {
        this(id, slot, tier, value, item, fluid, null, false);
    }

    PylonUpgrade(String id, Slot slot, PylonWire.Type transferType, boolean insert) {
        this(id, slot, 1, 0, null, null, transferType, insert);
    }

    PylonUpgrade(String id, Slot slot, int tier, int value, Supplier<Item> item, Supplier<Fluid> fluid,
                 PylonWire.Type transferType, boolean insert) {
        this.id = id;
        this.slot = slot;
        this.tier = tier;
        this.value = value;
        this.item = item;
        this.fluid = fluid;
        this.transferType = transferType;
        this.insert = insert;
    }

    public String id() { return id; }
    public Slot slot() { return slot; }
    public int tier() { return tier; }
    public int value() { return value; }
    public PylonWire.Type transferType() { return transferType; }
    public boolean importsResource() { return insert; }
    public String category() {
        return switch (slot) {
            case STORAGE, SPEED, QUANTITY, RANGE, ENERGY -> "modifier";
            case PROCESSING -> "processing";
            case GENERATOR -> "generator";
            case FILTER, ITEM_TRANSFER, FLUID_TRANSFER, ENERGY_TRANSFER, XP_TRANSFER, CHUNK_LOADER, SUPER, CREATIVE -> "utility";
            case SPECIALTY -> {
                if (this == FLUID_STORAGE || this == XP_TANK) yield "type";
                yield "utility";
            }
        };
    }
    public Item generatedItem() { return item == null ? Items.AIR : item.get(); }
    public Fluid generatedFluid() { return fluid == null ? Fluids.EMPTY : fluid.get(); }
    public boolean generatesFluid() { return fluid != null; }
    public boolean isGenerator() { return item != null || fluid != null; }
    public boolean isTank() { return this == FLUID_STORAGE || generatesFluid(); }
    public boolean locksItems() {
        return this == FLUID_STORAGE || this == XP_TANK ||
            this == FERTILIZER || this == HARVESTER || this == VOID;
    }

    public boolean isEnergy() { return slot == Slot.ENERGY; }

    public int energyTransfer() {
        return switch (this) {
            case ENERGY_1 -> 1_000;
            case ENERGY_2 -> 10_000;
            case ENERGY_3 -> 40_000;
            case ENERGY_4 -> 100_000;
            case ENERGY_5 -> 400_000;
            case ENERGY_6 -> Integer.MAX_VALUE;
            default -> 0;
        };
    }

    static long itemTransfer(int tier, long gameTime, int phase) {
        return switch (tier) {
            case 0, 1 -> {
                int interval = 4 >> tier;
                yield Math.floorMod(gameTime, interval) == Math.floorMod(phase, interval) ? 1 : 0;
            }
            case 2 -> 1;
            case 3 -> 16;
            case 4 -> 64;
            case 5 -> 256;
            case 6 -> Long.MAX_VALUE;
            default -> throw new IllegalArgumentException("Invalid Capacity tier: " + tier);
        };
    }

    static long fluidTransfer(int tier) {
        return switch (tier) {
            case 0 -> 250;
            case 1 -> 1_000;
            case 2 -> 2_000;
            case 3 -> 4_000;
            case 4 -> 16_000;
            case 5 -> 64_000;
            case 6 -> Long.MAX_VALUE;
            default -> throw new IllegalArgumentException("Invalid Capacity tier: " + tier);
        };
    }

    public static PylonUpgrade byId(String id) {
        for (PylonUpgrade upgrade : values()) {
            if (upgrade.id.equals(id)) return upgrade;
        }
        return null;
    }

    public static long itemCapacity(PylonUpgrade storage) {
        return storageCapacity(PylonContents.CAPACITY, storage);
    }

    static long storageCapacity(int baseCapacity, PylonUpgrade storage) {
        if (storage == STORAGE_6) return Long.MAX_VALUE;
        return (long) baseCapacity * (storage == null ? 1 : storage.value);
    }
}
