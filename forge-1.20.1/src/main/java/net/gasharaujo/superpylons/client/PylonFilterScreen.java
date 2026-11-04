package net.gasharaujo.superpylons.client;

import net.gasharaujo.superpylons.block.PylonFilterMenu;
import net.gasharaujo.superpylons.block.PylonFilterMenuBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

final class PylonFilterScreen extends AbstractContainerScreen<PylonFilterMenu> {
    private Button mode;

    PylonFilterScreen(PylonFilterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = PylonFilterMenuBase.HEIGHT;
        titleLabelY = 13;
        inventoryLabelY = PylonFilterMenuBase.INVENTORY_LABEL_Y;
    }

    @Override
    protected void init() {
        super.init();
        mode = addRenderableWidget(Button.builder(PylonFilterLayout.modeLabel(menu), button -> {
            if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        }).bounds(leftPos + 91, topPos + 7, 77, 20)
            .tooltip(Tooltip.create(Component.translatable("menu.gash_super_pylons.filter.help"))).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        mode.setMessage(PylonFilterLayout.modeLabel(menu));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float ticks, int mouseX, int mouseY) {
        PylonFilterLayout.draw(menu, leftPos, topPos, graphics::fill);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float ticks) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, ticks);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
