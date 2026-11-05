package net.gasharaujo.superpylons.block;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.util.List;
import java.util.Set;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
final class PylonWorkshopTest {
    @Test
    void rareOverlevelsChargeRarityAgainAndRejectUnsupportedEnchantments() {
        var combination = PylonEnchantmentRules.combine(Map.of("rare", 3), Map.of("rare", 3, "blocked", 1),
            e -> !e.equals("blocked"), (a, b) -> true, e -> 3, e -> 3);
        assertEquals(Map.of("rare", 4), combination.enchantments());
        assertEquals(36, combination.levels());
    }

    @Test
    void quantityStopsAtCompatibleAvailableEnchantmentsAndRespectsBlacklist() {
        var result = PylonEnchantmentRules.fill(List.of("a"), List.of("a", "b", "c", "blocked"),
            7, RandomSource.create(42), e -> e, e -> !e.equals("blocked"),
            (a, b) -> !(Set.of("a", "b").contains(a) && Set.of("a", "b").contains(b)));
        assertEquals(Set.of("a", "c"), Set.copyOf(result));
        assertEquals(2, result.size());
    }

}
