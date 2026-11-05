package net.gasharaujo.superpylons.block;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;

final class PylonEnchanting {
    private PylonEnchanting() {}

    static PylonWorkshopResult enchant(ServerLevel level, BlockPos pos, ItemStack input, int power,
                                       int minimum, ItemStack blacklist, List<ItemStack> shelves) {
        if (!input.isEnchantable()) return null;
        var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var excluded = enchantments(blacklist).keySet();
        var shelfRoll = PylonShelfEffects.select(level, pos, input, power, shelves, excluded, registry);
        List<EnchantmentInstance> chosen;
        List<EnchantmentInstance> available;
        if (shelfRoll != null) {
            chosen = shelfRoll.selected();
            available = shelfRoll.available();
            power = shelfRoll.power();
        } else {
            var eligible = registry.listElements().filter(e -> e.is(EnchantmentTags.IN_ENCHANTING_TABLE) && !excluded.contains(e)).toList();
            chosen = EnchantmentHelper.selectEnchantment(level.getRandom(), input, power, eligible.stream().map(e -> e));
            available = new java.util.ArrayList<>();
            for (var enchantment : eligible) {
                if (!input.isPrimaryItemFor(enchantment)) continue;
                for (int tier = enchantment.value().getMaxLevel(); tier >= enchantment.value().getMinLevel(); tier--) {
                    if (power >= enchantment.value().getMinCost(tier)) {
                        available.add(new EnchantmentInstance(enchantment, tier));
                        break;
                    }
                }
            }
        }
        var result = PylonEnchantmentRules.fill(chosen, available, minimum, level.getRandom(),
            e -> e.enchantment(), e -> !excluded.contains(e), Enchantment::areCompatible);
        if (result.isEmpty()) return null;
        ItemStack output = input.is(Items.BOOK) ? input.transmuteCopy(Items.ENCHANTED_BOOK, 1) : input.copyWithCount(1);
        var enchants = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(output));
        for (var enchantment : result) enchants.set(enchantment.enchantment(), enchantment.level());
        EnchantmentHelper.setEnchantments(output, enchants.toImmutable());
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
            donor.is(Items.ENCHANTED_BOOK) || donor.is(input.getItem()) ? enchantments(donor) : Map.<Holder<Enchantment>, Integer>of(),
            e -> book || e.value().canEnchant(input), Enchantment::areCompatible,
            e -> e.value().getMaxLevel(), e -> PylonEnchantmentRules.rarity(e.value().getWeight()));
        boolean repaired = !vanilla.isEmpty() && vanilla.getDamageValue() < input.getDamageValue();
        if (!combination.changed() && !repaired) return null;
        ItemStack output = vanilla.isEmpty() ? input.copyWithCount(1) : vanilla.copyWithCount(1);
        var enchants = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        combination.enchantments().forEach(enchants::set);
        EnchantmentHelper.setEnchantments(output, enchants.toImmutable());
        return new PylonWorkshopResult(output, combination.levels());
    }

    private static Map<Holder<Enchantment>, Integer> enchantments(ItemStack stack) {
        Map<Holder<Enchantment>, Integer> result = new LinkedHashMap<>();
        if (!stack.isEmpty()) for (var entry : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) {
            result.put(entry.getKey(), entry.getIntValue());
        }
        return result;
    }
}
