package net.gasharaujo.superpylons.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(EphemeralTestServerProvider.class)
final class PylonCrushRecipeTest {
    @Test
    void reversesQuartzAndAmethystCompactionAcrossQuantityTiers(MinecraftServer server) {
        for (int tier = 0; tier <= 6; tier++) {
            assertCrushResult(Items.QUARTZ_BLOCK, Items.QUARTZ, tier);
            assertCrushResult(Items.AMETHYST_BLOCK, Items.AMETHYST_SHARD, tier);
        }
        assertTrue(PylonPulverizing.find(List.of(), new ItemStack(Items.BUDDING_AMETHYST), 0).isEmpty());
    }

    private static void assertCrushResult(Item input, Item output, int tier) {
        var results = PylonPulverizing.find(List.of(), new ItemStack(input), tier);
        assertEquals(1, results.size());
        assertEquals(output, results.get(0).stack().getItem());
        assertEquals(4, results.get(0).stack().getCount());
        assertEquals(100, results.get(0).chance());
    }
}
