package net.gasharaujo.superpylons.client;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import net.gasharaujo.superpylons.block.PylonPulverizing;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public final class PylonCrushRecipes {
    public record Recipe(ItemStack input, List<PylonPulverizing.Result> outputs, List<Integer> tiers) {
        public String quantityLabel() {
            if (tiers.size() == 7) return "0\u2013VI";
            String[] labels = {"0", "I", "II", "III", "IV", "V", "VI"};
            return tiers.stream().map(tier -> labels[tier]).collect(Collectors.joining("/"));
        }
    }

    private static List<PylonPulverizing.Recipe> custom = List.of();
    private static ByteArrayOutputStream pending;
    private static int nextPart;
    private static int expectedParts;
    private static Runnable refresh;

    private PylonCrushRecipes() {}

    public static void receive(int part, int total, byte[] bytes) {
        if (part == 0) {
            pending = new ByteArrayOutputStream();
            nextPart = 0;
            expectedParts = total;
        }
        if (pending == null || total < 1 || total != expectedParts || part != nextPart || part >= total) {
            pending = null;
            return;
        }
        pending.writeBytes(bytes);
        nextPart++;
        if (nextPart == total) {
            custom = PylonPulverizing.importRecipes(pending.toString(StandardCharsets.UTF_8));
            pending = null;
            if (refresh != null) refresh.run();
        }
    }

    public static void onRefresh(Runnable listener) { refresh = listener; }

    public static void tagsUpdated() {
        if (refresh != null) refresh.run();
    }

    public static List<Recipe> recipes() {
        List<Recipe> recipes = new ArrayList<>();
        for (var item : BuiltInRegistries.ITEM) {
            ItemStack input = new ItemStack(item);
            if (input.isEmpty()) continue;
            var variants = new LinkedHashMap<String, Recipe>();
            for (int tier = 0; tier <= 6; tier++) {
                var outputs = PylonPulverizing.find(custom, input, tier);
                if (outputs.isEmpty()) continue;
                String key = outputs.stream().map(result -> BuiltInRegistries.ITEM.getKey(result.stack().getItem()) +
                    ":" + result.stack().getCount() + ":" + result.chance()).collect(Collectors.joining(","));
                var recipe = variants.computeIfAbsent(key, ignored -> new Recipe(input, outputs, new ArrayList<>()));
                recipe.tiers().add(tier);
            }
            for (var recipe : variants.values()) {
                recipes.add(new Recipe(recipe.input(), recipe.outputs(), List.copyOf(recipe.tiers())));
            }
        }
        return List.copyOf(recipes);
    }
}
