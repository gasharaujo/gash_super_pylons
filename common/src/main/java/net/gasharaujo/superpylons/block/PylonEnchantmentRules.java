package net.gasharaujo.superpylons.block;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import net.minecraft.util.RandomSource;

final class PylonEnchantmentRules {
    record Combination<E>(Map<E, Integer> enchantments, long levels, boolean changed) {}

    private PylonEnchantmentRules() {}

    static int rarity(int weight) {
        if (weight >= 10) return 1;
        if (weight >= 5) return 2;
        return weight >= 2 ? 3 : 4;
    }

    static <E, K> List<E> fill(List<E> selected, List<E> available, int minimum, RandomSource random,
                              Function<E, K> key, Predicate<K> allowed, BiPredicate<K, K> compatible) {
        List<E> result = new ArrayList<>();
        for (E enchantment : selected) {
            K candidate = key.apply(enchantment);
            if (allowed.test(candidate) && result.stream().allMatch(existing ->
                !key.apply(existing).equals(candidate) && compatible.test(key.apply(existing), candidate))) result.add(enchantment);
        }
        List<E> remaining = new ArrayList<>(available);
        while (result.size() < minimum && !remaining.isEmpty()) {
            E enchantment = remaining.remove(random.nextInt(remaining.size()));
            K candidate = key.apply(enchantment);
            if (allowed.test(candidate) && result.stream().allMatch(existing ->
                !key.apply(existing).equals(candidate) && compatible.test(key.apply(existing), candidate))) result.add(enchantment);
        }
        return result;
    }

    static <E> Combination<E> combine(Map<E, Integer> first, Map<E, Integer> second,
                                      Predicate<E> accepts, BiPredicate<E, E> compatible,
                                      ToIntFunction<E> maximum, ToIntFunction<E> rarity) {
        Map<E, Integer> combined = new LinkedHashMap<>(first);
        boolean changed = false;
        for (var entry : second.entrySet()) {
            E enchantment = entry.getKey();
            if (!accepts.test(enchantment) || combined.keySet().stream()
                .anyMatch(existing -> !existing.equals(enchantment) && !compatible.test(existing, enchantment))) continue;
            int previous = combined.getOrDefault(enchantment, 0);
            int incoming = entry.getValue();
            int next = previous == incoming ? Math.min(255, previous + 1) : Math.max(previous, incoming);
            if (next > previous) {
                combined.put(enchantment, next);
                changed = true;
            }
        }
        long levels = 0;
        for (var entry : combined.entrySet()) {
            int multiplier = rarity.applyAsInt(entry.getKey());
            long cost = (long) entry.getValue() * multiplier;
            if (entry.getValue() > maximum.applyAsInt(entry.getKey())) cost *= Math.max(2, multiplier);
            levels += cost;
        }
        return new Combination<>(combined, Math.max(1, levels), changed);
    }
}
