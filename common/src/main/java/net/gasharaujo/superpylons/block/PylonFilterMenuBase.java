package net.gasharaujo.superpylons.block;

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
    }

    public boolean isAllowList() { return mode.get() == 1; }

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
            getSlot(slot).set(button == 0 ? getCarried().copyWithCount(1) : ItemStack.EMPTY);
            if (pylon != null) pylon.setChanged();
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
