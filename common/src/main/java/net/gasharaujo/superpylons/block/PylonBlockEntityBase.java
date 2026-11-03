package net.gasharaujo.superpylons.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class PylonBlockEntityBase extends BlockEntity {
    protected final PylonContents contents = new PylonContents();

    protected PylonBlockEntityBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public int getStoredCount() { return contents.count(); }
    public ItemStack getStoredItem() { return contents.displayItem(); }

    public int insertFrom(ItemStack input) {
        if (level == null || level.isClientSide()) return 0;
        int inserted = contents.insert(input);
        if (inserted > 0) sync();
        return inserted;
    }

    public void withdraw(Player player) {
        if (level == null || level.isClientSide() || player.isSpectator()) return;
        ItemStack removed = contents.extract(player.isShiftKeyDown() ? Integer.MAX_VALUE : 1);
        if (removed.isEmpty()) return;
        sync();
        player.getInventory().placeItemBackInInventory(removed);
    }

    /** Drain before spawning drops so a removal cannot duplicate stored items. */
    public void dropContents() {
        if (level == null || level.isClientSide()) return;
        while (contents.count() > 0) {
            Block.popResource(level, worldPosition, contents.extract(Integer.MAX_VALUE));
        }
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
}
