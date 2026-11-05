package net.gasharaujo.superpylons.block;

import net.minecraft.core.Holder;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.core.component.DataComponents;

public final class PylonItemRules {
    public static final TagKey<Item> MODIFIER_ITEM = TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath("gsp", "modifier_item"));

    private PylonItemRules() {}

    public static boolean isModifier(ItemStack stack) { return stack.is(MODIFIER_ITEM); }

    static boolean isMiningTool(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof ShearsItem || stack.has(DataComponents.TOOL));
    }

    static boolean isWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        boolean[] damage = {false};
        stack.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.equals(Attributes.ATTACK_DAMAGE) && modifier.amount() > 0) damage[0] = true;
        });
        return damage[0];
    }

    static boolean matches(ItemStack stack, String selector) {
        if (!selector.startsWith("#")) return itemId(stack).equals(selector);
        ResourceLocation id = ResourceLocation.tryParse(selector.substring(1));
        return id != null && stack.is(TagKey.create(Registries.ITEM, id));
    }

    static String itemId(ItemStack stack) { return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(); }

    static ItemStack resolve(String selector, int count) {
        boolean tag = selector.startsWith("#");
        ResourceLocation id = ResourceLocation.tryParse(tag ? selector.substring(1) : selector);
        if (id == null) return ItemStack.EMPTY;
        if (!tag) return new ItemStack(BuiltInRegistries.ITEM.get(id), count);
        return BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, id))
            .flatMap(items -> items.stream().findFirst()).map(Holder::value)
            .map(item -> new ItemStack(item, count)).orElse(ItemStack.EMPTY);
    }

    static java.util.stream.Stream<String> tags(ItemStack stack) {
        return stack.getTags().map(tag -> tag.location().toString());
    }

    static List<ItemStack> tagItems(String selector) {
        var id = ResourceLocation.tryParse(selector.substring(1));
        if (id == null) return List.of();
        return BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, id))
            .map(items -> items.stream().map(Holder::value).map(ItemStack::new).toList()).orElse(List.of());
    }

    public static boolean exactlyEqual(ItemStack first, ItemStack second) {
        return ItemStack.isSameItemSameComponents(first, second);
    }
}
