package net.gasharaujo.superpylons.client;

import net.gasharaujo.superpylons.block.PylonFilterMenuBase;
import net.minecraft.network.chat.Component;

final class PylonFilterLayout {
    private PylonFilterLayout() {}

    static Component modeLabel(PylonFilterMenuBase menu) {
        return Component.translatable("menu.gash_super_pylons.filter." + (menu.isAllowList() ? "allow" : "block"));
    }

    static void draw(PylonFilterMenuBase menu, int x, int y, Fill fill) {
        fill.rectangle(x, y, x + 176, y + PylonFilterMenuBase.HEIGHT, 0xFF28282A);
        fill.rectangle(x + 2, y + 2, x + 174, y + PylonFilterMenuBase.HEIGHT - 2, 0xFFC6C6C6);
        for (int index = 0; index < menu.slots.size(); index++) {
            var slot = menu.getSlot(index);
            int left = x + slot.x, top = y + slot.y;
            fill.rectangle(left - 1, top - 1, left + 17, top + 17, 0xFFFFFFFF);
            fill.rectangle(left - 1, top - 1, left + 16, top + 16, 0xFF555555);
            fill.rectangle(left, top, left + 16, top + 16, 0xFF8B8B8B);
            if (index < menu.filterSize() && menu.isTagSelected(index)) {
                fill.rectangle(left, top + 15, left + 16, top + 17, 0xFFFFC445);
            }
        }
    }

    @FunctionalInterface
    interface Fill {
        void rectangle(int x0, int y0, int x1, int y1, int color);
    }
}
