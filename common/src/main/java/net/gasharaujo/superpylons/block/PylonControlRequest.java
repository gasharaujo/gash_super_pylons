package net.gasharaujo.superpylons.block;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public record PylonControlRequest(int menuId, int slot, ItemStack item, String tag) {
    public static PylonControlRequest wrench() { return new PylonControlRequest(-1, 0, ItemStack.EMPTY, ""); }

    public static PylonControlRequest item(int menuId, int slot, ItemStack item) {
        return new PylonControlRequest(menuId, slot, item.copyWithCount(1), "");
    }

    public static PylonControlRequest tag(int menuId, int slot, String tag) {
        return new PylonControlRequest(menuId, slot, ItemStack.EMPTY, tag);
    }

    public void handle(Player player) {
        if (menuId == -1) {
            PylonToolItem.toggleWrenchMode(player);
        } else if (player.containerMenu instanceof PylonFilterMenuBase filter && filter.containerId == menuId) {
            if (item.isEmpty()) filter.setGhostTag(player, slot, tag);
            else filter.setGhostItem(player, slot, item);
            filter.broadcastChanges();
        }
    }
}
