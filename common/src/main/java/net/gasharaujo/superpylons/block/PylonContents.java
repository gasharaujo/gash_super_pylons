package net.gasharaujo.superpylons.block;

import net.minecraft.world.item.ItemStack;

/** A one-item template plus a separate count; never creates oversized item stacks. */
public final class PylonContents {
    public static final int CAPACITY = 32 * 64;
    private ItemStack item = ItemStack.EMPTY;
    private long count;

    public long count() { return count; }
    public ItemStack displayItem() { return item.copy(); }

    public int insert(ItemStack input) {
        return insert(input, CAPACITY);
    }

    public int insert(ItemStack input, long capacity) {
        if (input.isEmpty() || input.getItem() instanceof PylonUpgradeItem || PylonItemRules.isModifier(input)) {
            return 0;
        }
        int accepted = add(input, input.getCount(), capacity);
        input.shrink(accepted);
        return accepted;
    }

    int add(ItemStack template, int amount, long capacity) {
        if (template.isEmpty() || amount <= 0 || count >= capacity ||
            (count > 0 && !PylonItemRules.exactlyEqual(item, template))) return 0;
        int accepted = (int) Math.min(amount, capacity - count);
        if (count == 0) item = template.copyWithCount(1);
        count += accepted;
        return accepted;
    }

    long moveTo(PylonContents target, long maximum, long capacity) {
        if (count == 0 || target.count >= capacity ||
            (target.count > 0 && !PylonItemRules.exactlyEqual(item, target.item))) return 0;
        long moved = Math.min(maximum, Math.min(count, capacity - target.count));
        if (moved <= 0) return 0;
        if (target.count == 0) target.item = item;
        target.count += moved;
        count -= moved;
        if (count == 0) item = ItemStack.EMPTY;
        return moved;
    }

    public ItemStack extract(int requested) {
        if (count == 0 || requested <= 0) return ItemStack.EMPTY;
        int taken = (int) Math.min(Math.min(requested, item.getMaxStackSize()), count);
        ItemStack result = item.copyWithCount(taken);
        count -= taken;
        if (count == 0) item = ItemStack.EMPTY;
        return result;
    }

    public void restore(ItemStack savedItem, long savedCount) {
        restore(savedItem, savedCount, CAPACITY);
    }

    public void restore(ItemStack savedItem, long savedCount, long capacity) {
        count = savedItem.isEmpty() ? 0 : Math.max(0, Math.min(capacity, savedCount));
        item = count == 0 ? ItemStack.EMPTY : savedItem.copyWithCount(1);
    }
}
