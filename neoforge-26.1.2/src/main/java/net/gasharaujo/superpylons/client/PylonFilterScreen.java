package net.gasharaujo.superpylons.client;

import net.gasharaujo.superpylons.PylonControls;
import net.gasharaujo.superpylons.block.PylonControlRequest;
import net.gasharaujo.superpylons.block.PylonFilterMenu;
import net.gasharaujo.superpylons.block.PylonFilterMenuBase;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;

final class PylonFilterScreen extends AbstractContainerScreen<PylonFilterMenu> {
    private Button mode;
    private PylonFilterTagPicker tagPicker;

    PylonFilterScreen(PylonFilterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, PylonFilterMenuBase.HEIGHT);
        titleLabelY = 13;
        inventoryLabelY = PylonFilterMenuBase.INVENTORY_LABEL_Y;
    }

    @Override
    protected void init() {
        super.init();
        tagPicker = new PylonFilterTagPicker(this::addWidget, this::removeWidget);
        mode = addRenderableWidget(Button.builder(PylonFilterLayout.modeLabel(menu), button -> {
            if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        }).bounds(leftPos + 91, topPos + 7, 77, 20)
            .tooltip(Tooltip.create(Component.translatable("menu.gash_super_pylons.filter.help"))).build());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 2 && tagPicker.open(menu, leftPos, topPos, event.x(), event.y())) return true;
        if (tagPicker.isOpen() && !tagPicker.overButton(event.x(), event.y())) return true;
        return super.mouseClicked(event, doubleClick);
    }

    boolean choosingTag() { return tagPicker.isOpen(); }

    Rect2i ghostArea(int slot) {
        var target = menu.getSlot(slot);
        return new Rect2i(leftPos + target.x, topPos + target.y, 16, 16);
    }

    void setGhost(int slot, ItemStack stack) {
        PylonControls.send(PylonControlRequest.item(menu.containerId, slot, stack));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        mode.setMessage(PylonFilterLayout.modeLabel(menu));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float ticks) {
        super.extractBackground(graphics, mouseX, mouseY, ticks);
        PylonFilterLayout.draw(menu, leftPos, topPos, graphics::fill);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float ticks) {
        super.extractRenderState(graphics, mouseX, mouseY, ticks);
        if (!tagPicker.isOpen()) return;
        graphics.nextStratum();
        tagPicker.render(graphics::fill, button -> button.extractRenderState(graphics, mouseX, mouseY, ticks));
    }
}
