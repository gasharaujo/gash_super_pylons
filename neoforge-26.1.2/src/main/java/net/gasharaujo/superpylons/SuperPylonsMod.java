package net.gasharaujo.superpylons;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import net.gasharaujo.superpylons.block.PylonUpgrade;
import net.gasharaujo.superpylons.block.PylonUpgradeItem;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import com.mojang.logging.LogUtils;
import net.gasharaujo.superpylons.block.SuperPylonBlock;
import net.gasharaujo.superpylons.block.SuperPylonBlockEntity;
import net.gasharaujo.superpylons.compat.PylonProbeProvider;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
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

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    public static final DeferredBlock<SuperPylonBlock> SUPER_PYLON = BLOCKS.registerBlock(
        "super_pylon", SuperPylonBlock::new,
        () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().explosionResistance(3600000.0F));
    public static final DeferredItem<BlockItem> SUPER_PYLON_ITEM = ITEMS.registerSimpleBlockItem(SUPER_PYLON);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SuperPylonBlockEntity>> SUPER_PYLON_ENTITY =
        BLOCK_ENTITIES.register("super_pylon", () ->
            new BlockEntityType<>(SuperPylonBlockEntity::new, SUPER_PYLON.get()));

    public static final Supplier<? extends Item> UPGRADE_BASE =
        ITEMS.registerSimpleItem("upgrade_base");
    public static final Map<PylonUpgrade, Supplier<? extends Item>> UPGRADES = registerUpgrades();

    private static Map<PylonUpgrade, Supplier<? extends Item>> registerUpgrades() {
        Map<PylonUpgrade, Supplier<? extends Item>> items = new EnumMap<>(PylonUpgrade.class);
        for (PylonUpgrade upgrade : PylonUpgrade.values()) {
            items.put(upgrade, ITEMS.registerItem(upgrade.id(), properties -> new PylonUpgradeItem(upgrade, properties)));
        }
        return items;
    }

    public SuperPylonsMod(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        bus.addListener(this::addCreativeItem);
        bus.addListener(this::enqueueIntegrations);
        bus.addListener(this::registerCapabilities);
        LOGGER.info("Gash's Super Pylons loaded ({})", MOD_ID);
    }

    private void enqueueIntegrations(InterModEnqueueEvent event) {
        if (ModList.get().isLoaded("theoneprobe")) {
            PylonProbeProvider.enqueueRegistration();
        }
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, SUPER_PYLON_ENTITY.get(),
            (pylon, side) -> pylon.getFluidHandler());
    }

    private void addCreativeItem(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(SUPER_PYLON_ITEM.get());
            event.accept(UPGRADE_BASE.get());
            UPGRADES.values().forEach(upgrade -> event.accept(upgrade.get()));
        }
    }
}
