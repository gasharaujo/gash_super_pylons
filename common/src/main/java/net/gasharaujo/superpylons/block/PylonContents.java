package net.gasharaujo.superpylons.block;

import net.minecraft.world.item.ItemStack;

/** A one-item template plus a separate count; never creates oversized item stacks. */
public final class PylonContents {
    public static final int CAPACITY = 32 * 64;
    private ItemStack item = ItemStack.EMPTY;
    private int count;

    public int count() { return count; }
    public ItemStack displayItem() { return item.copy(); }

    /** Moves items out of the supplied stack, including in Creative, preserving item conservation. */
    public int insert(ItemStack input) {
        if (input.isEmpty() || PylonItemRules.isModifier(input) ||
            (count > 0 && !PylonItemRules.exactlyEqual(item, input))) {
            return 0;
        }
        int accepted = Math.min(input.getCount(), CAPACITY - count);
        if (accepted <= 0) return 0;
        if (count == 0) item = input.copyWithCount(1);
        count += accepted;
        input.shrink(accepted);
        return accepted;
    }

    public ItemStack extract(int requested) {
        if (count == 0 || requested <= 0) return ItemStack.EMPTY;
        int taken = Math.min(Math.min(requested, item.getMaxStackSize()), count);
        ItemStack result = item.copyWithCount(taken);
        count -= taken;
        if (count == 0) item = ItemStack.EMPTY;
        return result;
    }

    public void restore(ItemStack savedItem, int savedCount) {
        count = savedItem.isEmpty() ? 0 : Math.max(0, Math.min(CAPACITY, savedCount));
        item = count == 0 ? ItemStack.EMPTY : savedItem.copyWithCount(1);
    }
}
