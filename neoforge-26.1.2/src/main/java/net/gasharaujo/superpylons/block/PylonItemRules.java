package net.gasharaujo.superpylons.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class PylonItemRules {
    public static final TagKey<Item> MODIFIER_ITEM = TagKey.create(Registries.ITEM,
        Identifier.fromNamespaceAndPath("gsp", "modifier_item"));

    private PylonItemRules() {}

    public static boolean isModifier(ItemStack stack) { return stack.is(MODIFIER_ITEM); }

    public static boolean exactlyEqual(ItemStack first, ItemStack second) {
        return ItemStack.isSameItemSameComponents(first, second);
    }
}
