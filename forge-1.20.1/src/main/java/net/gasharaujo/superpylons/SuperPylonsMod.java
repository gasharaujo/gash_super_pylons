package net.gasharaujo.superpylons;

import com.mojang.logging.LogUtils;
import net.gasharaujo.superpylons.block.SuperPylonBlock;
import net.gasharaujo.superpylons.block.SuperPylonBlockEntity;
import net.gasharaujo.superpylons.block.SuperPylonBlockItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
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

    public SuperPylonsMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        bus.addListener(this::addCreativeItem);
        LOGGER.info("Gash's Super Pylons loaded ({})", MOD_ID);
    }

    private void addCreativeItem(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(SUPER_PYLON_ITEM.get());
        }
    }
}
