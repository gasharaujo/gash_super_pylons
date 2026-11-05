package net.gasharaujo.superpylons.block;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

final class PylonDispenser {
    private static final DefaultDispenseItemBehavior DEFAULT_BEHAVIOR = new DefaultDispenseItemBehavior();

    private PylonDispenser() {}

    static List<ItemStack> dispense(ServerLevel level, BlockPos origin, Direction direction, ItemStack stack) {
        DispenseItemBehavior behavior = stack.isItemEnabled(level.enabledFeatures())
            ? DispenserBlock.DISPENSER_REGISTRY.get(stack.getItem()) : null;
        if (behavior == null) behavior = DEFAULT_BEHAVIOR;
        if (behavior == DispenseItemBehavior.NOOP) return List.of(stack);
        BlockState state = Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, direction);
        var inventory = new DispenserBlockEntity(origin, state);
        inventory.setLevel(level);
        var source = new BlockSource(level, origin, state, inventory);
        DispenseItemBehavior selected = behavior;
        var remainders = new ArrayList<ItemStack>();
        ItemStack remainder = PylonDispenseAim.withDirection(level, direction, () -> selected.dispense(source, stack));
        if (!remainder.isEmpty()) remainders.add(remainder);
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack extra = inventory.removeItemNoUpdate(slot);
            if (!extra.isEmpty()) remainders.add(extra);
        }
        return remainders;
    }
}
