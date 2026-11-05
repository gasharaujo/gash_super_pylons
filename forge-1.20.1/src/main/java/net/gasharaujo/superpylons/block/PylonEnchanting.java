package net.gasharaujo.superpylons.block;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

final class PylonEnchanting {
    private PylonEnchanting() {}

    static PylonWorkshopResult enchant(ServerLevel level, BlockPos pos, ItemStack input, int power,
                                       int minimum, ItemStack blacklist, List<ItemStack> shelves) {
        if (!input.isEnchantable()) return null;
        Object registry = null;
        var excluded = enchantments(blacklist).keySet();
        var shelfRoll = PylonShelfEffects.select(level, pos, input, power, shelves, excluded, registry);
        List<EnchantmentInstance> chosen;
        List<EnchantmentInstance> available;
        if (shelfRoll != null) {
            chosen = shelfRoll.selected();
            available = shelfRoll.available();
            power = shelfRoll.power();
        } else {
            chosen = EnchantmentHelper.selectEnchantment(level.getRandom(), input, power, false);
            available = new java.util.ArrayList<>();
            for (var enchantment : BuiltInRegistries.ENCHANTMENT) {
                if (enchantment.isTreasureOnly() || !enchantment.isDiscoverable() ||
                    !(input.is(Items.BOOK) || enchantment.canApplyAtEnchantingTable(input))) continue;
                for (int tier = enchantment.getMaxLevel(); tier >= enchantment.getMinLevel(); tier--) {
                    if (power >= enchantment.getMinCost(tier)) {
                        available.add(new EnchantmentInstance(enchantment, tier));
                        break;
                    }
                }
            }
        }
        var result = PylonEnchantmentRules.fill(chosen, available, minimum, level.getRandom(),
            e -> e.enchantment, e -> !excluded.contains(e), Enchantment::isCompatibleWith);
        if (result.isEmpty()) return null;
        ItemStack output = input.is(Items.BOOK) ? new ItemStack(Items.ENCHANTED_BOOK) : input.copyWithCount(1);
        if (input.is(Items.BOOK) && input.hasTag()) output.setTag(input.getTag().copy());
        Map<Enchantment, Integer> enchants = new LinkedHashMap<>(EnchantmentHelper.getEnchantments(output));
        for (var enchantment : result) enchants.put(enchantment.enchantment, enchantment.level);
        EnchantmentHelper.setEnchantments(enchants, output);
        return new PylonWorkshopResult(output, 3);
    }

    static PylonWorkshopResult anvil(ServerPlayer player, ItemStack input, ItemStack donor) {
        if (input.isEmpty() || donor.isEmpty()) return null;
        boolean previousCreative = player.getAbilities().instabuild;
        ItemStack vanilla;
        try {
            player.getAbilities().instabuild = true;
            AnvilMenu menu = new AnvilMenu(0, player.getInventory(), ContainerLevelAccess.NULL);
            menu.getSlot(0).set(input.copyWithCount(1));
            menu.getSlot(1).set(donor.copyWithCount(1));
            menu.createResult();
            vanilla = menu.getSlot(2).getItem().copy();
        } finally {
            player.getAbilities().instabuild = previousCreative;
        }
        boolean book = input.is(Items.ENCHANTED_BOOK);
        var combination = PylonEnchantmentRules.combine(enchantments(input),
            donor.is(Items.ENCHANTED_BOOK) || donor.is(input.getItem()) ? enchantments(donor) : Map.<Enchantment, Integer>of(),
            e -> book || e.canEnchant(input), Enchantment::isCompatibleWith,
            Enchantment::getMaxLevel, e -> e.getRarity().ordinal() + 1);
        boolean repaired = !vanilla.isEmpty() && vanilla.getDamageValue() < input.getDamageValue();
        if (!combination.changed() && !repaired) return null;
        ItemStack output = vanilla.isEmpty() ? input.copyWithCount(1) : vanilla.copyWithCount(1);
        EnchantmentHelper.setEnchantments(combination.enchantments(), output);
        return new PylonWorkshopResult(output, combination.levels());
    }

    private static Map<Enchantment, Integer> enchantments(ItemStack stack) {
        return stack.isEmpty() ? Map.of() : EnchantmentHelper.getEnchantments(stack);
    }
}
