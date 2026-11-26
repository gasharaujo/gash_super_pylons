package net.gasharaujo.superpylons.block;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.gasharaujo.superpylons.compat.PylonAgriCraft;
import net.gasharaujo.superpylons.compat.PylonTreeChop;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

final class PylonFarming {
    record Harvest(BlockState remainder, List<ItemStack> drops) {}
    interface BonemealGrowth {
        boolean apply(ServerLevel level, BlockPos pos, BlockState state, RandomSource random,
                      BonemealableBlock plant, boolean force);
    }

    private PylonFarming() {}

    static boolean canPlant(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item)) return false;
        BlockState state = item.getBlock().defaultBlockState();
        return state.is(BlockTags.SAPLINGS) || state.getBlock() instanceof CropBlock ||
            state.getBlock() instanceof StemBlock || state.is(Blocks.NETHER_WART) || fungusSoil(state) != null;
    }

    private static boolean canGrow(ServerLevel level, BlockPos pos, BlockState state) {
        Block soil = fungusSoil(state);
        if (soil != null) return level.getBlockState(pos.below()).is(soil);
        Block block = state.getBlock();
        return state.isRandomlyTicking() && (block instanceof CropBlock || block instanceof GrowingPlantHeadBlock ||
            state.is(BlockTags.CROPS) || state.is(BlockTags.SAPLINGS) || state.is(Blocks.COCOA) ||
            state.is(Blocks.NETHER_WART) || state.is(Blocks.PUMPKIN_STEM) || state.is(Blocks.MELON_STEM) ||
            state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.SUGAR_CANE) || state.is(Blocks.CACTUS) ||
            state.is(Blocks.BAMBOO) || state.is(Blocks.BAMBOO_SAPLING) || state.is(Blocks.CHORUS_FLOWER));
    }

    static boolean canGrow(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return canGrow(level, pos, state) || hasAgriCraftPlant(level, pos, state);
    }

    static boolean grow(ServerLevel level, BlockPos pos, boolean force, RandomSource random, BonemealGrowth bonemeal) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BonemealableBlock plant && hasAgriCraftPlant(level, pos, state)) {
            return bonemeal.apply(level, pos, state, random, plant, false);
        }
        if (!canGrow(level, pos, state)) return false;
        if (fungusSoil(state) != null && state.getBlock() instanceof BonemealableBlock fungus) {
            bonemeal.apply(level, pos, state, random, fungus, force);
            return true;
        }
        if (force) return advanceStage(level, pos, state);
        state.randomTick(level, pos, random);
        return true;
    }

    private static boolean hasAgriCraftPlant(ServerLevel level, BlockPos pos, BlockState state) {
        return state.hasBlockEntity() && state.getBlock() instanceof BonemealableBlock &&
            PylonAgriCraft.hasPlant(level.getBlockEntity(pos));
    }

    private static Block fungusSoil(BlockState state) {
        if (state.is(Blocks.CRIMSON_FUNGUS)) return Blocks.CRIMSON_NYLIUM;
        if (state.is(Blocks.WARPED_FUNGUS)) return Blocks.WARPED_NYLIUM;
        return null;
    }

    private static boolean advanceStage(ServerLevel level, BlockPos pos, BlockState state) {
        for (var property : state.getProperties()) {
            if (!(property instanceof IntegerProperty stage) ||
                !(stage.getName().equals("age") || stage.getName().equals("stage"))) continue;
            int next = state.getValue(stage) + 1;
            if (stage.getPossibleValues().contains(next)) return level.setBlockAndUpdate(pos, state.setValue(stage, next));
        }
        if (state.getBlock() instanceof SaplingBlock sapling) {
            sapling.advanceTree(level, pos, state, level.getRandom());
            return true;
        }
        return false;
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

    static boolean isTreeLog(ServerLevel level, BlockPos pos, BlockState state) {
        return PylonTreeChop.originalState(level, pos, state).is(BlockTags.LOGS);
    }

    private static BlockState treeState(ServerLevel level, BlockPos pos) {
        return PylonTreeChop.originalState(level, pos, level.getBlockState(pos));
    }

    static List<BlockPos> tree(ServerLevel level, BlockPos root) {
        BlockState state = treeState(level, root);
        if (state.is(Blocks.CRIMSON_STEM)) {
            return hugeFungus(level, root, Blocks.CRIMSON_STEM, Blocks.CRIMSON_NYLIUM, Blocks.NETHER_WART_BLOCK);
        }
        if (state.is(Blocks.WARPED_STEM)) {
            return hugeFungus(level, root, Blocks.WARPED_STEM, Blocks.WARPED_NYLIUM, Blocks.WARPED_WART_BLOCK);
        }
        if (!state.is(BlockTags.LOGS) || treeState(level, root.below()).is(BlockTags.LOGS)) {
            return List.of();
        }
        var tree = new ArrayList<BlockPos>();
        var found = new HashSet<BlockPos>();
        tree.add(root.immutable());
        found.add(root.immutable());
        for (int i = 0; i < tree.size(); i++) {
            BlockPos log = tree.get(i);
            for (BlockPos next : BlockPos.betweenClosed(log.offset(-1, -1, -1), log.offset(1, 1, 1))) {
                if (next.getY() < root.getY() || !level.hasChunkAt(next) ||
                    !treeState(level, next).is(BlockTags.LOGS) || !found.add(next.immutable())) continue;
                if (tree.size() >= 256) return List.of();
                tree.add(next.immutable());
            }
        }
        int logs = tree.size();
        for (int i = 0; i < tree.size(); i++) {
            BlockPos part = tree.get(i);
            int distance = i < logs ? 0 : level.getBlockState(part).getValue(LeavesBlock.DISTANCE);
            for (Direction direction : Direction.values()) {
                BlockPos next = part.relative(direction);
                if (!level.hasChunkAt(next)) continue;
                BlockState leaf = level.getBlockState(next);
                if (!(leaf.getBlock() instanceof LeavesBlock) || leaf.getValue(LeavesBlock.PERSISTENT) ||
                    leaf.getValue(LeavesBlock.DISTANCE) != distance + 1 || !found.add(next)) continue;
                if (tree.size() >= 4096) return List.of();
                tree.add(next);
            }
        }
        return tree.size() == logs ? List.of() : tree;
    }

    private static List<BlockPos> hugeFungus(ServerLevel level, BlockPos root, Block stem, Block soil, Block wart) {
        if (!level.getBlockState(root.below()).is(soil)) return List.of();
        var parts = new ArrayList<BlockPos>();
        var found = new HashSet<BlockPos>();
        BlockPos tip = root;
        while (treeState(level, tip).is(stem)) {
            if (parts.size() == 32) return List.of();
            parts.add(tip.immutable());
            found.add(tip.immutable());
            tip = tip.above();
        }
        int stems = parts.size();
        for (int i = 0; i < parts.size(); i++) {
            BlockPos part = parts.get(i);
            for (BlockPos next : BlockPos.betweenClosed(part.offset(-1, -1, -1), part.offset(1, 1, 1))) {
                if (next.getY() < root.getY() || next.getY() > tip.getY() ||
                    Math.abs(next.getX() - root.getX()) > 3 || Math.abs(next.getZ() - root.getZ()) > 3 ||
                    !level.hasChunkAt(next)) continue;
                BlockState cap = level.getBlockState(next);
                if ((cap.is(wart) || cap.is(Blocks.SHROOMLIGHT)) && found.add(next.immutable())) {
                    parts.add(next.immutable());
                }
            }
        }
        return parts.size() == stems ? List.of() : parts;
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
