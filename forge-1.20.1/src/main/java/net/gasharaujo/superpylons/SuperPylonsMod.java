package net.gasharaujo.superpylons;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import net.gasharaujo.superpylons.block.PylonFilterMenu;
import net.gasharaujo.superpylons.block.PylonToolActions.Kind;
import net.gasharaujo.superpylons.block.PylonToolItem;
import net.gasharaujo.superpylons.block.PylonUpgrade;
import net.gasharaujo.superpylons.block.PylonUpgradeItem;

import com.mojang.logging.LogUtils;
import net.gasharaujo.superpylons.block.SuperPylonBlock;
import net.gasharaujo.superpylons.block.SuperPylonBlockEntity;
import net.gasharaujo.superpylons.compat.PylonProbeProvider;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.gasharaujo.superpylons.block.SuperPylonBlockItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.slf4j.Logger;

@Mod(SuperPylonsMod.MOD_ID)
public final class SuperPylonsMod {
    public static final String MOD_ID = "gash_super_pylons";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);
    public static final RegistryObject<SuperPylonBlock> SUPER_PYLON = BLOCKS.register("super_pylon", () ->
        new SuperPylonBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion().explosionResistance(3600000.0F)));
    public static final RegistryObject<BlockItem> SUPER_PYLON_ITEM = ITEMS.register("super_pylon", () ->
        new SuperPylonBlockItem(SUPER_PYLON.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<SuperPylonBlockEntity>> SUPER_PYLON_ENTITY =
        BLOCK_ENTITIES.register("super_pylon", () ->
            BlockEntityType.Builder.of(SuperPylonBlockEntity::new, SUPER_PYLON.get()).build(null));

    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MOD_ID);
    public static final RegistryObject<MenuType<PylonFilterMenu>> FILTER_MENU = MENUS.register("pylon_filter",
        () -> new MenuType<>(PylonFilterMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final Supplier<? extends Item> WRENCH =
        ITEMS.register("wrench", () -> new PylonToolItem(Kind.WRENCH, new Item.Properties()));
    public static final Supplier<? extends Item> PAINT_TOOL =
        ITEMS.register("paint_tool", () -> new PylonToolItem(Kind.PAINT, new Item.Properties()));
    public static final Supplier<? extends Item> COPY_PASTE_TOOL =
        ITEMS.register("copy_paste_tool", () -> new PylonToolItem(Kind.COPY, new Item.Properties()));
    public static final Supplier<? extends Item> ENERGY_WIRE =
        ITEMS.register("energy_wire", () -> new PylonToolItem(Kind.ENERGY, new Item.Properties()));
    public static final Supplier<? extends Item> ITEM_WIRE =
        ITEMS.register("item_wire", () -> new PylonToolItem(Kind.ITEM, new Item.Properties()));
    public static final Supplier<? extends Item> FLUID_WIRE =
        ITEMS.register("fluid_wire", () -> new PylonToolItem(Kind.FLUID, new Item.Properties()));
    public static final Supplier<? extends Item> UPGRADE_BASE =
        ITEMS.register("upgrade_base", () -> new Item(new Item.Properties()));
    public static final Map<PylonUpgrade, Supplier<? extends Item>> UPGRADES = registerUpgrades();

    private static Map<PylonUpgrade, Supplier<? extends Item>> registerUpgrades() {
        Map<PylonUpgrade, Supplier<? extends Item>> items = new EnumMap<>(PylonUpgrade.class);
        for (PylonUpgrade upgrade : PylonUpgrade.values()) {
            items.put(upgrade, ITEMS.register(upgrade.id(), () -> new PylonUpgradeItem(upgrade, new Item.Properties())));
        }
        return items;
    }

    public SuperPylonsMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        bus.addListener(this::addCreativeItem);
        bus.addListener(this::enqueueIntegrations);
        LOGGER.info("Gash's Super Pylons loaded ({})", MOD_ID);
    }

    private void enqueueIntegrations(InterModEnqueueEvent event) {
        if (ModList.get().isLoaded("theoneprobe")) {
            PylonProbeProvider.enqueueRegistration();
        }
    }


    private void addCreativeItem(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(SUPER_PYLON_ITEM.get());
            event.accept(UPGRADE_BASE.get());
            event.accept(WRENCH.get());
            event.accept(PAINT_TOOL.get());
            event.accept(COPY_PASTE_TOOL.get());
            event.accept(ENERGY_WIRE.get());
            event.accept(ITEM_WIRE.get());
            event.accept(FLUID_WIRE.get());
            UPGRADES.values().forEach(upgrade -> event.accept(upgrade.get()));
        }
    }
}
