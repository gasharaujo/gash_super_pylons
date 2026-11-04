package net.gasharaujo.superpylons.client;

import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;

@JeiPlugin
public final class PylonJeiPlugin implements IModPlugin {
    @Override public Identifier getPluginUid() { return Identifier.fromNamespaceAndPath(SuperPylonsMod.MOD_ID, "jei"); }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(PylonFilterScreen.class, new IGhostIngredientHandler<>() {
            @Override
            public <I> List<Target<I>> getTargetsTyped(PylonFilterScreen screen, ITypedIngredient<I> ingredient,
                                                       boolean doStart) {
                var stack = ingredient.getItemStack().orElse(null);
                if (stack == null || stack.isEmpty() || screen.choosingTag()) return List.of();
                var targets = new ArrayList<Target<I>>();
                for (int i = 0; i < screen.getMenu().filterSize(); i++) {
                    int slot = i;
                    targets.add(new Target<I>() {
                        @Override public Rect2i getArea() { return screen.ghostArea(slot); }
                        @Override public void accept(I ignored) { screen.setGhost(slot, stack); }
                    });
                }
                return targets;
            }

            @Override public void onComplete() {}
        });
    }
}
