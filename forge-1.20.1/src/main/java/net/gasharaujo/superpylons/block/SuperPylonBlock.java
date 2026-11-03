package net.gasharaujo.superpylons.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class SuperPylonBlock extends BaseEntityBlock {
    // The twelve bars from the supplied model. The floating core has no collision.
    private static final VoxelShape FRAME = Shapes.or(
        Block.box(0, 0, 0, 2, 2, 16), Block.box(14, 0, 0, 16, 2, 16),
        Block.box(0, 12, 0, 2, 14, 16), Block.box(14, 12, 0, 16, 14, 16),
        Block.box(0, 2, 0, 2, 12, 2), Block.box(14, 2, 0, 16, 12, 2),
        Block.box(0, 2, 14, 2, 12, 16), Block.box(14, 2, 14, 16, 12, 16),
        Block.box(2, 0, 0, 14, 2, 2), Block.box(2, 0, 14, 14, 2, 16),
        Block.box(2, 12, 0, 14, 14, 2), Block.box(2, 12, 14, 14, 14, 16)
    );

    public SuperPylonBlock(Properties properties) { super(properties); }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SuperPylonBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FRAME;
    }
}
