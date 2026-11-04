package net.gasharaujo.superpylons.block;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public final class PylonUpgradeItem extends Item {
    private final PylonUpgrade upgrade;

    public PylonUpgradeItem(PylonUpgrade upgrade, Properties properties) {
        super(properties);
        this.upgrade = upgrade;
    }

    public PylonUpgrade upgrade() { return upgrade; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable("tooltip.gash_super_pylons.category." + upgrade.category())
            .withStyle(ChatFormatting.BLUE));
    }
}
