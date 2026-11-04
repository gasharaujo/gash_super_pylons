package net.gasharaujo.superpylons.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.gasharaujo.superpylons.PylonControls;
import net.gasharaujo.superpylons.block.PylonControlRequest;
import net.gasharaujo.superpylons.block.PylonFilterMenuBase;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

final class PylonFilterTagPicker {
    private static final int TAGS_PER_PAGE = 6;
    private final List<Button> buttons = new ArrayList<>();
    private final Consumer<Button> add;
    private final Consumer<Button> remove;
    private int left;
    private int top;

    PylonFilterTagPicker(Consumer<Button> add, Consumer<Button> remove) {
        this.add = add;
        this.remove = remove;
    }

    boolean open(PylonFilterMenuBase menu, int x, int y, double mouseX, double mouseY) {
        for (int i = 0; i < menu.filterSize(); i++) {
            var slot = menu.getSlot(i);
            double dx = mouseX - x - slot.x;
            double dy = mouseY - y - slot.y;
            if (dx >= 0 && dx < 16 && dy >= 0 && dy < 16 && !slot.getItem().isEmpty()) {
                show(menu, i, menu.availableTags(i), x, y, 0);
                return true;
            }
        }
        return false;
    }

    private void show(PylonFilterMenuBase menu, int slot, List<String> tags, int x, int y, int page) {
        close();
        left = x + 5;
        top = y + 25;
        button(Component.translatable("menu.gash_super_pylons.filter.exact"), x + 8, y + 28, 160,
            () -> select(menu, slot, ""));
        int first = page * TAGS_PER_PAGE;
        for (int i = 0; i < TAGS_PER_PAGE && first + i < tags.size(); i++) {
            String tag = tags.get(first + i);
            String label = tag.startsWith("minecraft:") ? tag.substring("minecraft:".length()) : tag;
            button(Component.literal("#" + label), x + 8, y + 47 + i * 18, 160, () -> select(menu, slot, tag))
                .setTooltip(Tooltip.create(Component.literal("#" + tag)));
        }
        if (page > 0) button(Component.literal("<"), x + 8, y + 158, 23, () -> show(menu, slot, tags, x, y, page - 1));
        if (first + TAGS_PER_PAGE < tags.size()) {
            button(Component.literal(">"), x + 145, y + 158, 23, () -> show(menu, slot, tags, x, y, page + 1));
        }
        button(Component.translatable("gui.cancel"), x + 38, y + 158, 100, this::close);
    }

    private void select(PylonFilterMenuBase menu, int slot, String tag) {
        PylonControls.send(PylonControlRequest.tag(menu.containerId, slot, tag));
        close();
    }

    private Button button(Component label, int x, int y, int width, Runnable action) {
        Button button = Button.builder(label, ignored -> action.run()).bounds(x, y, width, 17)
            .tooltip(Tooltip.create(label)).build();
        buttons.add(button);
        add.accept(button);
        return button;
    }

    boolean isOpen() { return !buttons.isEmpty(); }

    boolean overButton(double x, double y) { return buttons.stream().anyMatch(button -> button.isMouseOver(x, y)); }

    void render(PylonFilterLayout.Fill fill, Consumer<Button> drawButton) {
        fill.rectangle(left, top, left + 166, top + 153, 0xFF242428);
        fill.rectangle(left + 1, top + 1, left + 165, top + 152, 0xFFB9B9BD);
        buttons.forEach(drawButton);
    }

    private void close() {
        buttons.forEach(remove);
        buttons.clear();
    }
}
