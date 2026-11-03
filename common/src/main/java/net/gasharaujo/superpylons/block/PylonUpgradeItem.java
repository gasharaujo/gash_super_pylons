package net.gasharaujo.superpylons.block;

import net.minecraft.world.item.Item;

public final class PylonUpgradeItem extends Item {
    private final PylonUpgrade upgrade;

    public PylonUpgradeItem(PylonUpgrade upgrade, Properties properties) {
        super(properties);
        this.upgrade = upgrade;
    }

    public PylonUpgrade upgrade() { return upgrade; }
}
