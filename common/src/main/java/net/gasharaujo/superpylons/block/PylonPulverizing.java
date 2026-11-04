package net.gasharaujo.superpylons.block;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;

final class PylonPulverizing {
    private static final int[] FLINT_CHANCE = {30, 40, 50, 60, 80, 90, 100};
    private static final int[] RAW_DUST = {2, 2, 2, 2, 4, 4, 6};
    private static final int[] ORE_YIELD = {2, 2, 3, 3, 4, 4, 5};
    private static final Map<ResourceManager, List<Recipe>> RECIPES = new WeakHashMap<>();

    record Result(ItemStack stack, int chance) {}

    private record Output(String selector, int[] counts, int[] chances) {}
    private record Recipe(String ingredient, List<Output> outputs) {}

    private PylonPulverizing() {}

    static List<Result> find(ServerLevel server, ItemStack input, int tier) {
        for (Recipe recipe : customRecipes(server.getServer().getResourceManager())) {
            if (!PylonItemRules.matches(input, recipe.ingredient())) continue;
            List<Result> results = new ArrayList<>();
            for (Output output : recipe.outputs()) {
                append(results, PylonItemRules.resolve(output.selector(), output.counts()[tier]), output.chances()[tier]);
            }
            return results;
        }

        String item = PylonItemRules.itemId(input);
        String path = item.substring(item.indexOf(':') + 1);
        if (path.equals("gravel") || inCommonTag(input, "gravel")) {
            return List.of(result("minecraft:sand", 1, 100), result("minecraft:flint", 1, FLINT_CHANCE[tier]));
        }
        if (path.equals("cobblestone") || path.startsWith("cobbled_") ||
            inCommonTag(input, "cobblestones") || inCommonTag(input, "cobbles")) {
            return single("minecraft:gravel", 1);
        }
        if (path.equals("terracotta") || path.endsWith("_terracotta") && !path.endsWith("_glazed_terracotta") ||
            PylonItemRules.matches(input, "#minecraft:terracotta") || inCommonTag(input, "terracotta")) {
            return single("minecraft:red_sand", 1);
        }
        if (path.equals("stone")) return single("minecraft:cobblestone", 1);
        if (path.equals("deepslate")) return single("minecraft:cobbled_deepslate", 1);
        if (inCommonTag(input, "stones") || inCommonTag(input, "stone")) {
            String namespace = item.substring(0, item.indexOf(':') + 1);
            var cobbled = first(1, namespace + "cobbled_" + path, namespace + path + "_cobblestone",
                namespace + "cobblestone_" + path, namespace + path + "_cobble");
            if (!cobbled.isEmpty()) return List.of(new Result(cobbled, 100));
        }

        List<String> tags = PylonItemRules.tags(input).sorted().toList();
        String ore = material(tags, "ores");
        if (ore == null && path.endsWith("_ore")) {
            ore = path.substring(0, path.length() - 4).replaceFirst("^deepslate_", "").replaceFirst("^nether_", "");
        }
        if (ore != null) return ore(ore, item, ORE_YIELD[tier]);

        String raw = material(tags, "raw_materials", "raw_ores");
        if (raw == null && path.startsWith("raw_")) raw = path.substring(4);
        if (raw != null) return dust(raw, item, RAW_DUST[tier]);
        String refined = material(tags, "ingots", "gems");
        if (refined == null && path.endsWith("_ingot")) refined = path.substring(0, path.length() - 6);
        if (refined == null && (inCommonTag(input, "gems") ||
            List.of("diamond", "emerald", "lapis_lazuli", "quartz", "amethyst_shard").contains(path))) refined = path;
        return refined == null ? List.of() : dust(refined, item, 1);
    }

    private static List<Result> ore(String material, String item, int amount) {
        String namespace = item.substring(0, item.indexOf(':') + 1);
        return switch (material) {
            case "redstone" -> single("minecraft:redstone", amount * 6);
            case "lapis", "lapis_lazuli" -> single("minecraft:lapis_lazuli", amount * 6);
            default -> results(first(amount, "#c:raw_materials/" + material, "#c:raw_ores/" + material,
                "#forge:raw_materials/" + material, namespace + "raw_" + material, "minecraft:raw_" + material,
                "#c:gems/" + material, "#forge:gems/" + material, namespace + material, "minecraft:" + material));
        };
    }

    private static List<Result> dust(String material, String item, int amount) {
        String namespace = item.substring(0, item.indexOf(':') + 1);
        ItemStack dust = first(amount, "#c:dust/" + material, "#c:dusts/" + material, "#forge:dusts/" + material);
        if (!dust.isEmpty()) return results(dust);
        dust = first(amount, namespace + material + "_dust", namespace + "dust_" + material);
        if (!dust.isEmpty() && (inCommonTag(dust, "dust") || inCommonTag(dust, "dusts"))) return results(dust);
        for (String tag : List.of("#c:dust", "#c:dusts", "#forge:dusts")) {
            for (ItemStack candidate : PylonItemRules.tagItems(tag)) {
                String id = PylonItemRules.itemId(candidate);
                if (id.endsWith(":" + material + "_dust") || id.endsWith(":dust_" + material)) {
                    return results(candidate.copyWithCount(amount));
                }
            }
        }
        return List.of();
    }

    private static String material(List<String> tags, String... families) {
        for (String family : families) {
            for (String tag : tags) {
                if (tag.startsWith("c:" + family + "/") || tag.startsWith("forge:" + family + "/")) {
                    return tag.substring(tag.indexOf('/') + 1);
                }
            }
        }
        return null;
    }

    private static boolean inCommonTag(ItemStack stack, String tag) {
        return PylonItemRules.matches(stack, "#c:" + tag) || PylonItemRules.matches(stack, "#forge:" + tag);
    }

    private static ItemStack first(int amount, String... selectors) {
        for (String selector : selectors) {
            ItemStack stack = PylonItemRules.resolve(selector, amount);
            if (!stack.isEmpty()) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static Result result(String item, int amount, int chance) {
        return new Result(PylonItemRules.resolve(item, amount), chance);
    }

    private static List<Result> single(String item, int amount) {
        return results(PylonItemRules.resolve(item, amount));
    }

    private static List<Result> results(ItemStack stack) {
        return stack.isEmpty() ? List.of() : List.of(new Result(stack, 100));
    }

    private static void append(List<Result> results, ItemStack stack, int chance) {
        if (!stack.isEmpty() && chance > 0) results.add(new Result(stack, chance));
    }

    private static synchronized List<Recipe> customRecipes(ResourceManager resources) {
        return RECIPES.computeIfAbsent(resources, PylonPulverizing::load);
    }

    private static List<Recipe> load(ResourceManager resources) {
        List<Recipe> recipes = new ArrayList<>();
        var files = resources.listResources("pulverizing", id -> id.getPath().endsWith(".json"));
        files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(file -> {
            try (var reader = file.getValue().openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                List<Output> outputs = new ArrayList<>();
                for (JsonElement entry : json.getAsJsonArray("results")) {
                    JsonObject output = entry.getAsJsonObject();
                    if (outputs.size() == 8) throw new IllegalArgumentException("At most 8 results are allowed");
                    outputs.add(new Output(output.get("item").getAsString(),
                        tiers(output.get("count"), 1, 64), tiers(output.get("chance"), 100, 100)));
                }
                recipes.add(new Recipe(json.get("ingredient").getAsString(), List.copyOf(outputs)));
            } catch (IOException | RuntimeException ex) {
                LogUtils.getLogger().warn("Skipping Pulverize recipe {}", file.getKey(), ex);
            }
        });
        return List.copyOf(recipes);
    }

    private static int[] tiers(JsonElement value, int fallback, int max) {
        if (value != null && value.isJsonArray() && value.getAsJsonArray().size() != 7) {
            throw new IllegalArgumentException("Tier arrays must have exactly seven entries");
        }
        int[] result = new int[7];
        for (int i = 0; i < result.length; i++) {
            int entry = value == null ? fallback : (value.isJsonArray() ? value.getAsJsonArray().get(i) : value).getAsInt();
            if (entry < 0 || entry > max) throw new IllegalArgumentException("Count or chance outside allowed bounds");
            result[i] = entry;
        }
        return result;
    }
}
