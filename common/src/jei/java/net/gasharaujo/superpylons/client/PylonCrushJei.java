package net.gasharaujo.superpylons.client;

import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.gasharaujo.superpylons.block.PylonUpgrade;
import net.minecraft.network.chat.Component;

final class PylonCrushJei {
    static final RecipeType<PylonCrushRecipes.Recipe> TYPE =
        RecipeType.create(SuperPylonsMod.MOD_ID, "crushing", PylonCrushRecipes.Recipe.class);
    private static List<PylonCrushRecipes.Recipe> current = List.of();
    private static IJeiRuntime runtime;

    private PylonCrushJei() {}

    static void categories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new PylonCrushCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    static void recipes(IRecipeRegistration registration) {
        current = PylonCrushRecipes.recipes();
        registration.addRecipes(TYPE, current);
    }

    static void catalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(TYPE, SuperPylonsMod.SUPER_PYLON_ITEM.get(),
            SuperPylonsMod.UPGRADES.get(PylonUpgrade.PULVERIZE).get());
    }

    static void available(IJeiRuntime value) {
        runtime = value;
        PylonCrushRecipes.onRefresh(PylonCrushJei::refresh);
    }

    static void unavailable() {
        PylonCrushRecipes.onRefresh(null);
        runtime = null;
        current = List.of();
    }

    private static void refresh() {
        if (runtime == null) return;
        var recipes = PylonCrushRecipes.recipes();
        runtime.getRecipeManager().hideRecipes(TYPE, current);
        runtime.getRecipeManager().addRecipes(TYPE, recipes);
        current = recipes;
    }

    static class Category implements IRecipeCategory<PylonCrushRecipes.Recipe> {
        private final IDrawable icon;

        Category(IGuiHelper gui) {
            icon = gui.createDrawableItemLike(SuperPylonsMod.UPGRADES.get(PylonUpgrade.PULVERIZE).get());
        }

        @Override public RecipeType<PylonCrushRecipes.Recipe> getRecipeType() { return TYPE; }
        @Override public Component getTitle() { return Component.translatable("jei.gash_super_pylons.crushing"); }
        @Override public IDrawable getIcon() { return icon; }
        @Override public int getWidth() { return 164; }
        @Override public int getHeight() { return 108; }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, PylonCrushRecipes.Recipe recipe, IFocusGroup focuses) {
            builder.addInputSlot(8, 30).setStandardSlotBackground().addItemStack(recipe.input());
            for (int i = 0; i < recipe.outputs().size(); i++) {
                var result = recipe.outputs().get(i);
                builder.addOutputSlot(66 + i % 4 * 24, 17 + i / 4 * 35).setStandardSlotBackground()
                    .addItemStack(result.stack())
                    .addRichTooltipCallback((view, tooltip) ->
                        tooltip.add(Component.translatable("jei.gash_super_pylons.chance", result.chance())));
            }
        }

    }
}
