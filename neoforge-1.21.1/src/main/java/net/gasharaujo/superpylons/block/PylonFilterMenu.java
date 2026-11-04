package net.gasharaujo.superpylons.block;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;

public final class PylonFilterMenu extends PylonFilterMenuBase {
    public PylonFilterMenu(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    public PylonFilterMenu(int id, Inventory inventory, PylonBlockEntityBase pylon) {
        super(id, inventory, pylon);
    }

    @Override
    public void clicked(int slot, int button, ClickType input, Player player) {
        if (!handleGhost(slot, button, input == ClickType.PICKUP, player)) super.clicked(slot, button, input, player);
    }
}
