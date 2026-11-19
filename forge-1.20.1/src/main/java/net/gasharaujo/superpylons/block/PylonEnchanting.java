package net.gasharaujo.superpylons.block;

import java.util.ArrayList;
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
            donor.is(Items.ENCHANTED_BOOK) || donor.is(input.getItem()) ? enchantments(donor) : Map.<Enchantment, Integer>of(),
            e -> book || e.canEnchant(input), Enchantment::isCompatibleWith,
            Enchantment::getMaxLevel, PylonEnchanting::bookCost);
        boolean repaired = !vanilla.isEmpty() && vanilla.getDamageValue() < input.getDamageValue();
        if (!combination.changed() && !repaired) return null;
        ItemStack output = vanilla.isEmpty() ? input.copyWithCount(1) : vanilla.copyWithCount(1);
        EnchantmentHelper.setEnchantments(combination.enchantments(), output);
        return new PylonWorkshopResult(output, combination.levels());
    }

    private static int bookCost(Enchantment e) { return e.getRarity().ordinal() + 1; }

    static PylonGrindstoneResult grindstone(ServerLevel level, ItemStack input, boolean capture, int perBook) {
        var original = enchantments(input);
        Map<Enchantment, Integer> kept = new LinkedHashMap<>();
        Map<Enchantment, Integer> captured = new LinkedHashMap<>();
        List<ItemStack> books = new ArrayList<>();
        int cost = 0;
        for (var enchant : original.entrySet()) {
            var enchantment = enchant.getKey();
            if (!capture && enchantment.isCurse()) {
                kept.put(enchantment, enchant.getValue());
            } else if (capture) {
                captured.put(enchantment, enchant.getValue());
                if (captured.size() == perBook) {
                    books.add(capturedBook(captured));
                    captured.clear();
                }
            } else {
                cost += enchantment.getMinCost(enchant.getValue());
            }
        }
        if (!captured.isEmpty()) books.add(capturedBook(captured));
        if (original.size() == kept.size()) return null;
        ItemStack output = input.is(Items.ENCHANTED_BOOK) && kept.isEmpty() ?
            new ItemStack(Items.BOOK) : input.copyWithCount(1);
        if (output.is(Items.BOOK) && input.hasTag()) {
            output.setTag(input.getTag().copy());
            output.removeTagKey("StoredEnchantments");
        }
        output.removeTagKey("RepairCost");
        setEnchantments(output, kept);
        int half = (cost + 1) / 2;
        int experience = half <= 0 ? 0 : half + level.getRandom().nextInt(half);
        return new PylonGrindstoneResult(output, books, experience);
    }

    private static ItemStack capturedBook(Map<Enchantment, Integer> enchantments) {
        var book = new ItemStack(Items.ENCHANTED_BOOK);
        setEnchantments(book, enchantments);
        return book;
    }

    private static void setEnchantments(ItemStack stack, Map<Enchantment, Integer> enchants) {
        EnchantmentHelper.setEnchantments(enchants, stack);
    }

    private static Map<Enchantment, Integer> enchantments(ItemStack stack) {
        return stack.isEmpty() ? Map.of() : EnchantmentHelper.getEnchantments(stack);
    }
}
