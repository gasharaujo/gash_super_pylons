package net.gasharaujo.superpylons.block;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import net.minecraft.core.component.DataComponents;
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
        return anvil(player, input, donor, false);
    }

    static PylonWorkshopResult anvil(ServerPlayer player, ItemStack input, ItemStack donor, boolean superUpgrade) {
        if (input.isEmpty() || donor.isEmpty()) return null;
        if (superUpgrade && input.is(Items.BOOK) && donor.is(Items.ENCHANTED_BOOK)) {
            var donorEnchantments = enchantments(donor);
            if (donorEnchantments.isEmpty()) return null;
            long cost = donorEnchantments.entrySet().stream()
                .mapToLong(enchant -> (long) bookCost(enchant.getKey()) * enchant.getValue()).sum();
            return new PylonWorkshopResult(donor.copyWithCount(1), cost);
        }
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
            e -> e.value().getMaxLevel(), PylonEnchanting::bookCost);
        boolean repaired = !vanilla.isEmpty() && vanilla.getDamageValue() < input.getDamageValue();
        if (!combination.changed() && !repaired) return null;
        ItemStack output = vanilla.isEmpty() ? input.copyWithCount(1) : vanilla.copyWithCount(1);
        var enchants = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        combination.enchantments().forEach(enchants::set);
        EnchantmentHelper.setEnchantments(output, enchants.toImmutable());
        return new PylonWorkshopResult(output, combination.levels());
    }

    private static int bookCost(Holder<Enchantment> e) { return PylonEnchantmentRules.rarity(e.value().getWeight()); }

    static PylonGrindstoneResult grindstone(ServerLevel level, ItemStack input, boolean capture, int perBook) {
        var original = enchantments(input);
        Map<Holder<Enchantment>, Integer> kept = new LinkedHashMap<>();
        Map<Holder<Enchantment>, Integer> captured = new LinkedHashMap<>();
        List<ItemStack> books = new ArrayList<>();
        int cost = 0;
        for (var enchant : original.entrySet()) {
            var enchantment = enchant.getKey();
            if (!capture && enchantment.is(EnchantmentTags.CURSE)) {
                kept.put(enchantment, enchant.getValue());
            } else if (capture) {
                captured.put(enchantment, enchant.getValue());
                if (captured.size() == perBook) {
                    books.add(capturedBook(captured));
                    captured.clear();
                }
            } else {
                cost += enchantment.value().getMinCost(enchant.getValue());
            }
        }
        if (!captured.isEmpty()) books.add(capturedBook(captured));
        if (original.size() == kept.size()) return null;
        ItemStack output = input.is(Items.ENCHANTED_BOOK) && kept.isEmpty() ?
            input.transmuteCopy(Items.BOOK, 1) : input.copyWithCount(1);
        output.remove(DataComponents.REPAIR_COST);
        if (output.is(Items.BOOK)) output.remove(DataComponents.STORED_ENCHANTMENTS);
        setEnchantments(output, kept);
        int half = (cost + 1) / 2;
        int experience = half <= 0 ? 0 : half + level.getRandom().nextInt(half);
        return new PylonGrindstoneResult(output, books, experience);
    }

    private static ItemStack capturedBook(Map<Holder<Enchantment>, Integer> enchantments) {
        var book = new ItemStack(Items.ENCHANTED_BOOK);
        setEnchantments(book, enchantments);
        return book;
    }

    private static void setEnchantments(ItemStack stack, Map<Holder<Enchantment>, Integer> enchants) {
        var mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        enchants.forEach(mutable::set);
        EnchantmentHelper.setEnchantments(stack, mutable.toImmutable());
    }

    private static Map<Holder<Enchantment>, Integer> enchantments(ItemStack stack) {
        Map<Holder<Enchantment>, Integer> result = new LinkedHashMap<>();
        if (!stack.isEmpty()) for (var entry : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) {
            result.put(entry.getKey(), entry.getIntValue());
        }
        return result;
    }
}
