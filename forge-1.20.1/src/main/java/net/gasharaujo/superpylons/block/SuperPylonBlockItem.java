package net.gasharaujo.superpylons.block;

import java.util.function.Consumer;
import net.gasharaujo.superpylons.client.SuperPylonItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public final class SuperPylonBlockItem extends BlockItem {
    public SuperPylonBlockItem(Block block, Properties properties) { super(block, properties); }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final SuperPylonItemRenderer renderer = new SuperPylonItemRenderer();
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() { return renderer; }
        });
    }
}
