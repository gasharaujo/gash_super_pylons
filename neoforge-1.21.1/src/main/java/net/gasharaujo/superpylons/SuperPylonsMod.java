package net.gasharaujo.superpylons;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import net.gasharaujo.superpylons.block.PylonFilterMenu;
import net.gasharaujo.superpylons.block.PylonToolActions.Kind;
import net.gasharaujo.superpylons.block.PylonToolItem;
import net.gasharaujo.superpylons.block.PylonUpgrade;
import net.gasharaujo.superpylons.block.PylonExperienceStorage;
import net.gasharaujo.superpylons.block.PylonBreaker;
import net.gasharaujo.superpylons.block.PylonDispenseAim;
import net.gasharaujo.superpylons.block.PylonUpgradeItem;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import com.mojang.logging.LogUtils;
import net.gasharaujo.superpylons.block.SuperPylonBlock;
import net.gasharaujo.superpylons.block.SuperPylonBlockEntity;
import net.gasharaujo.superpylons.compat.PylonProbeProvider;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.fml.common.Mod;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.slf4j.Logger;

@Mod(SuperPylonsMod.MOD_ID)
public final class SuperPylonsMod {
    public static final String MOD_ID = "gash_super_pylons";
    public static final BlockCapability<PylonExperienceStorage, Direction> EXPERIENCE = BlockCapability.createSided(
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "experience"), PylonExperienceStorage.class);
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    public static final DeferredBlock<SuperPylonBlock> SUPER_PYLON = BLOCKS.registerBlock(
        "super_pylon", SuperPylonBlock::new,
        BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().explosionResistance(3600000.0F));
    public static final DeferredItem<BlockItem> SUPER_PYLON_ITEM = ITEMS.registerSimpleBlockItem(SUPER_PYLON);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SuperPylonBlockEntity>> SUPER_PYLON_ENTITY =
        BLOCK_ENTITIES.register("super_pylon", () ->
            BlockEntityType.Builder.of(SuperPylonBlockEntity::new, SUPER_PYLON.get()).build(null));

    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MOD_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<PylonFilterMenu>> FILTER_MENU = MENUS.register("pylon_filter",
        () -> new MenuType<>(PylonFilterMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final Supplier<? extends Item> WRENCH =
        ITEMS.registerItem("wrench", properties -> new PylonToolItem(Kind.WRENCH, properties));
    public static final Supplier<? extends Item> PAINT_TOOL =
        ITEMS.registerItem("paint_tool", properties -> new PylonToolItem(Kind.PAINT, properties));
    public static final Supplier<? extends Item> COPY_PASTE_TOOL =
        ITEMS.registerItem("copy_paste_tool", properties -> new PylonToolItem(Kind.COPY, properties));
    public static final Supplier<? extends Item> ENERGY_WIRE =
        ITEMS.registerItem("energy_wire", properties -> new PylonToolItem(Kind.ENERGY, properties));
    public static final Supplier<? extends Item> ITEM_WIRE =
        ITEMS.registerItem("item_wire", properties -> new PylonToolItem(Kind.ITEM, properties));
    public static final Supplier<? extends Item> FLUID_WIRE =
        ITEMS.registerItem("fluid_wire", properties -> new PylonToolItem(Kind.FLUID, properties));
    public static final Supplier<? extends Item> XP_WIRE =
        ITEMS.registerItem("xp_wire", properties -> new PylonToolItem(Kind.XP, properties));
    public static final Supplier<? extends Item> UPGRADE_BASE =
        ITEMS.registerSimpleItem("upgrade_base");
    public static final Map<PylonUpgrade, Supplier<? extends Item>> UPGRADES = registerUpgrades();

    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    public static final Supplier<CreativeModeTab> CREATIVE_TAB = CREATIVE_TABS.register("main", () -> CreativeModeTab.builder()
        .title(Component.translatable("item_group.gash_super_pylons.main"))
        .icon(() -> new ItemStack(SUPER_PYLON_ITEM.get()))
        .displayItems((parameters, output) -> {
            ITEMS.getEntries().forEach(item -> output.accept(item.get()));
            if (parameters.hasPermissions()) output.accept(net.minecraft.world.item.Items.COMMAND_BLOCK);
        })
        .build());

    private static Map<PylonUpgrade, Supplier<? extends Item>> registerUpgrades() {
        Map<PylonUpgrade, Supplier<? extends Item>> items = new EnumMap<>(PylonUpgrade.class);
        for (PylonUpgrade upgrade : PylonUpgrade.values()) {
            if (upgrade == PylonUpgrade.CREATIVE_STORAGE) {
                items.put(upgrade, () -> net.minecraft.world.item.Items.COMMAND_BLOCK);
                continue;
            }
            items.put(upgrade, ITEMS.registerItem(upgrade.id(), properties -> new PylonUpgradeItem(upgrade, properties)));
        }
        return items;
    }

    public SuperPylonsMod(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        CREATIVE_TABS.register(bus);
        bus.addListener(PylonControls::register);
        bus.addListener((RegisterTicketControllersEvent event) -> event.register(SuperPylonBlockEntity.CHUNK_TICKETS));
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,
            (EntityJoinLevelEvent event) -> {
                PylonBreaker.trackDrop(event.getLevel(), event.getEntity());
                PylonDispenseAim.onSpawn(event.getLevel(), event.getEntity());
            });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::installWorkTool);
        bus.addListener(this::enqueueIntegrations);
        bus.addListener(this::registerCapabilities);
        LOGGER.info("Gash's Super Pylons loaded ({})", MOD_ID);
    }

    private void installWorkTool(PlayerInteractEvent.RightClickBlock event) {
        if (event.getUseBlock() == TriState.FALSE || event.getUseItem() == TriState.FALSE) return;
        var level = event.getLevel();
        if (level.getBlockEntity(event.getPos()) instanceof SuperPylonBlockEntity pylon &&
            pylon.tryInstallWorkTool(event.getEntity(), event.getHand())) {
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
            event.setCanceled(true);
        }
    }

    private void enqueueIntegrations(InterModEnqueueEvent event) {
        if (ModList.get().isLoaded("theoneprobe")) {
            PylonProbeProvider.enqueueRegistration();
        }
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(EXPERIENCE, SUPER_PYLON_ENTITY.get(), (pylon, side) -> pylon);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SUPER_PYLON_ENTITY.get(),
            (pylon, side) -> pylon.getItemHandler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, SUPER_PYLON_ENTITY.get(),
            (pylon, side) -> pylon.getFluidHandler());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, SUPER_PYLON_ENTITY.get(),
            SuperPylonBlockEntity::getEnergyHandler);
    }
}
