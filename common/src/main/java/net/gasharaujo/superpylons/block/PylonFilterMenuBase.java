package net.gasharaujo.superpylons.block;

import java.util.List;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public abstract class PylonFilterMenuBase extends AbstractContainerMenu {
    public static final int HEIGHT = 190;
    public static final int INVENTORY_LABEL_Y = 100;
    private final PylonBlockEntityBase pylon;
    private final DataSlot mode;
    private final DataSlot[] tags = new DataSlot[PylonBlockEntityBase.FILTER_SIZE];

    protected PylonFilterMenuBase(int id, Inventory inventory, PylonBlockEntityBase pylon) {
        super(SuperPylonsMod.FILTER_MENU.get(), id);
        this.pylon = pylon;
        var ghost = pylon == null ? new SimpleContainer(PylonBlockEntityBase.FILTER_SIZE) : pylon.filter;
        for (int i = 0; i < PylonBlockEntityBase.FILTER_SIZE; i++) {
            addSlot(new Slot(ghost, i, 8 + (i % 9) * 18, 34 + (i / 9) * 18) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
                @Override public boolean mayPickup(Player player) { return false; }
            });
        }
        for (int i = 0; i < 27; i++) {
            addSlot(new Slot(inventory, i + 9, 8 + (i % 9) * 18, 112 + (i / 9) * 18));
        }
        for (int i = 0; i < 9; i++) addSlot(new Slot(inventory, i, 8 + i * 18, 170));

        mode = addDataSlot(pylon == null ? DataSlot.standalone() : new DataSlot() {
            @Override public int get() { return pylon.allowList ? 1 : 0; }
            @Override public void set(int value) { pylon.allowList = value == 1; }
        });
        for (int i = 0; i < tags.length; i++) {
            int slot = i;
            tags[i] = addDataSlot(pylon == null ? DataSlot.standalone() : new DataSlot() {
                @Override public int get() {
                    String tag = pylon.filterTag(slot);
                    return tag == null ? 0 : availableTags(slot).indexOf(tag) + 1;
                }
                @Override public void set(int value) {}
            });
        }
    }

    public boolean isAllowList() { return mode.get() == 1; }

    public int filterSize() { return tags.length; }

    public List<String> availableTags(int slot) {
        if (slot < 0 || slot >= tags.length || getSlot(slot).getItem().isEmpty()) return List.of();
        return PylonItemRules.tags(getSlot(slot).getItem()).sorted().toList();
    }

    public boolean isTagSelected(int slot) { return tags[slot].get() > 0; }

    public void setGhostItem(Player player, int slot, ItemStack item) {
        if (pylon == null || slot < 0 || slot >= tags.length || !stillValid(player)) return;
        getSlot(slot).set(item.copyWithCount(1));
        pylon.setFilterTag(slot, null);
    }

    public void setGhostTag(Player player, int slot, String tag) {
        if (pylon == null || slot < 0 || slot >= tags.length || !stillValid(player)) return;
        if (tag.isEmpty() || availableTags(slot).contains(tag)) pylon.setFilterTag(slot, tag.isEmpty() ? null : tag);
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (button != 0 || pylon == null || !stillValid(player)) return false;
        pylon.allowList = !pylon.allowList;
        pylon.setChanged();
        return true;
    }

    protected boolean handleGhost(int slot, int button, boolean pickup, Player player) {
        if (slot < 0 || slot >= PylonBlockEntityBase.FILTER_SIZE) return false;
        if (pickup && (button == 0 || button == 1) && stillValid(player)) {
            ItemStack item = button == 0 ? getCarried() : ItemStack.EMPTY;
            if (pylon == null) getSlot(slot).set(item.copyWithCount(1));
            else setGhostItem(player, slot, item);
        }
        return true;
    }

    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) {
        return pylon == null || (!player.isSpectator() && !pylon.isRemoved() && pylon.hasFilter() &&
            pylon.getLevel() == player.level() && player.distanceToSqr(Vec3.atCenterOf(pylon.getBlockPos())) <= 64);
    }
}
