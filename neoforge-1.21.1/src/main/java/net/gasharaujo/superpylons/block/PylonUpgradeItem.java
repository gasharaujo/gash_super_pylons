package net.gasharaujo.superpylons.block;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class PylonUpgradeItem extends Item {
    private final PylonUpgrade upgrade;

    public PylonUpgradeItem(PylonUpgrade upgrade, Properties properties) {
        super(properties);
        this.upgrade = upgrade;
    }

    public PylonUpgrade upgrade() { return upgrade; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.gash_super_pylons.category." + upgrade.category())
            .withStyle(ChatFormatting.BLUE));
        if (upgrade == PylonUpgrade.ENCHANTING) {
            lines.add(Component.translatable("tooltip.gash_super_pylons.enchanting_quantity_requires_super"));
        }
    }
}
