package net.gasharaujo.superpylons.client;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

final class PylonCrushCategory extends PylonCrushJei.Category {
    private final IDrawable arrow;

    PylonCrushCategory(IGuiHelper gui) {
        super(gui);
        arrow = gui.getRecipeArrow();
    }

    @Override
    public void draw(PylonCrushRecipes.Recipe recipe, IRecipeSlotsView slots, GuiGraphics graphics,
                     double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        arrow.draw(graphics, 33, 30);
        graphics.drawString(font, Component.translatable("jei.gash_super_pylons.quantity", recipe.quantityLabel()),
            0, 0, 0xFF404040, false);
        graphics.drawWordWrap(font, Component.translatable("jei.gash_super_pylons.cost"),
            0, 84, 164, 0xFF404040);
        for (int i = 0; i < recipe.outputs().size(); i++) {
            int chance = recipe.outputs().get(i).chance();
            if (chance < 100) {
                graphics.drawString(font, chance + "%", 64 + i % 4 * 24, 35 + i / 4 * 35, 0xFF404040, false);
            }
        }
    }
}
