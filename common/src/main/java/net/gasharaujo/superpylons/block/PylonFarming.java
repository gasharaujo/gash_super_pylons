package net.gasharaujo.superpylons.block;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

final class PylonFarming {
    record Harvest(BlockState remainder, List<ItemStack> drops) {}

    private PylonFarming() {}

    static boolean canGrow(BlockState state) {
        Block block = state.getBlock();
        return state.isRandomlyTicking() && (block instanceof CropBlock || block instanceof GrowingPlantHeadBlock ||
            state.is(BlockTags.CROPS) || state.is(BlockTags.SAPLINGS) || state.is(Blocks.COCOA) ||
            state.is(Blocks.NETHER_WART) || state.is(Blocks.PUMPKIN_STEM) || state.is(Blocks.MELON_STEM) ||
            state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.SUGAR_CANE) || state.is(Blocks.CACTUS) ||
            state.is(Blocks.BAMBOO) || state.is(Blocks.BAMBOO_SAPLING) || state.is(Blocks.CHORUS_FLOWER));
    }

    static Harvest harvest(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
            return withLoot(level, pos, state, crop.getStateForAge(0));
        }
        if (state.is(Blocks.NETHER_WART) && state.getValue(NetherWartBlock.AGE) == 3) {
            return withLoot(level, pos, state, state.setValue(NetherWartBlock.AGE, 0));
        }
        if (state.is(Blocks.COCOA) && state.getValue(CocoaBlock.AGE) == 2) {
            return withLoot(level, pos, state, state.setValue(CocoaBlock.AGE, 0));
        }
        if (state.is(Blocks.SWEET_BERRY_BUSH) && state.getValue(SweetBerryBushBlock.AGE) >= 2) {
            int berries = 1 + level.getRandom().nextInt(2) + (state.getValue(SweetBerryBushBlock.AGE) == 3 ? 1 : 0);
            return new Harvest(state.setValue(SweetBerryBushBlock.AGE, 1), List.of(new ItemStack(Items.SWEET_BERRIES, berries)));
        }
        if (CaveVines.hasGlowBerries(state)) {
            return new Harvest(state.setValue(CaveVines.BERRIES, false), List.of(new ItemStack(Items.GLOW_BERRIES)));
        }
        if ((state.is(Blocks.MELON) && hasStem(level, pos, Blocks.ATTACHED_MELON_STEM)) ||
            (state.is(Blocks.PUMPKIN) && hasStem(level, pos, Blocks.ATTACHED_PUMPKIN_STEM)) ||
            isColumnTip(level, pos, state)) {
            return withLoot(level, pos, state, Blocks.AIR.defaultBlockState());
        }
        return null;
    }

    private static Harvest withLoot(ServerLevel level, BlockPos pos, BlockState state, BlockState remainder) {
        return new Harvest(remainder, Block.getDrops(state, level, pos, null));
    }

    private static boolean hasStem(ServerLevel level, BlockPos fruit, Block stem) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = fruit.relative(direction);
            if (!level.hasChunkAt(neighbor)) continue;
            BlockState state = level.getBlockState(neighbor);
            if (state.is(stem) && state.getValue(BlockStateProperties.HORIZONTAL_FACING) == direction.getOpposite()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isColumnTip(ServerLevel level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        return (block == Blocks.SUGAR_CANE || block == Blocks.CACTUS || block == Blocks.BAMBOO) &&
            level.getBlockState(pos.below()).is(block) && !level.getBlockState(pos.above()).is(block);
    }
}
