package net.gasharaujo.superpylons.compat;

import com.mojang.logging.LogUtils;
import de.scc.calculator.api.SccMachines;
import de.scc.calculator.api.SccNetwork;
import de.scc.calculator.api.SccRecipeProviders;
import de.scc.calculator.network.CableBusBlockEntity;
import de.scc.calculator.network.ProcessingCalculatorBlockEntity;
import java.util.Set;
import java.util.function.BiFunction;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.gasharaujo.superpylons.block.PylonPulverizing;
import net.gasharaujo.superpylons.block.PylonUpgrade;
import net.gasharaujo.superpylons.block.SuperPylonBlockEntity;
import net.gasharaujo.superpylons.client.PylonCrushRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

public final class PylonSccCompat {
    private static final String CRUSH = SuperPylonsMod.MOD_ID + ":crushing";
    private static final String CRUSH_PREFIX = CRUSH + "/";
    private static final String[] QUANTITY_LABELS = {"0", "I", "II", "III", "IV", "V", "VI"};
    private static final String CRUSH_RECIPE_FORMAT = "quantity-categories-1:";
    private static boolean registered;

    private PylonSccCompat() {}

    public static void register() {
        var scc = ModList.get().getModContainerById("scc");
        if (scc.isEmpty() ||
            scc.get().getModInfo().getVersion().compareTo(new DefaultArtifactVersion("1.6.5")) < 0) return;
        try {
            Api.register();
            registered = true;
        } catch (LinkageError error) {
            LogUtils.getLogger().warn("The installed SCC version does not provide the Super Pylon integration API", error);
        }
    }

    public static boolean usesCalculatorOutput(Level level, BlockPos pylon, BlockPos target) {
        return registered && Api.usesCalculatorOutput(level, pylon, target);
    }

    public static BiFunction<ItemStack, Boolean, ItemStack> itemOutput(Level level, BlockPos pylon, BlockPos target) {
        return registered ? Api.itemOutput(level, pylon, target) : null;
    }

    public static Set<String> processingCategories(Level level, BlockPos pos) {
        var pylon = pylonAt(level, pos);
        if (pylon == null) return null;
        String category = processingCategory(pylon);
        return category == null ? Set.of() : Set.of(category);
    }

    public static boolean acceptsLegacyCrushCategory(Level level, BlockPos pos, String category) {
        if (!CRUSH.equals(category)) return false;
        var pylon = pylonAt(level, pos);
        return pylon != null && pylon.canUseProcessingCalculator() && pylon.getProcessing() == PylonUpgrade.PULVERIZE;
    }

    public static Component processingLabel(Level level, BlockPos pos) {
        var pylon = pylonAt(level, pos);
        if (pylon == null || !pylon.canUseProcessingCalculator()) return null;
        if (pylon.getProcessing() == PylonUpgrade.PULVERIZE) {
            return Component.translatable("gui.gash_super_pylons.scc.crush", QUANTITY_LABELS[pylon.processingQuantityTier()]);
        }
        var type = pylon.smeltingType();
        if (type == RecipeType.BLASTING) return Component.translatable("gui.gash_super_pylons.scc.blasting");
        if (type == RecipeType.SMOKING) return Component.translatable("gui.gash_super_pylons.scc.smoking");
        return Component.translatable("gui.gash_super_pylons.scc.smelt");
    }

    public static ItemStack processingIcon(Level level, BlockPos pos) {
        var pylon = pylonAt(level, pos);
        if (pylon == null || !pylon.canUseProcessingCalculator()) return ItemStack.EMPTY;
        return new ItemStack(SuperPylonsMod.UPGRADES.get(pylon.getProcessing()).get());
    }

    private static SuperPylonBlockEntity pylonAt(Level level, BlockPos pos) {
        if (!registered || level == null || !level.hasChunkAt(pos)) return null;
        return level.getBlockEntity(pos) instanceof SuperPylonBlockEntity pylon ? pylon : null;
    }

    private static String processingCategory(SuperPylonBlockEntity pylon) {
        if (!pylon.canUseProcessingCalculator()) return null;
        return switch (pylon.getProcessing()) {
            case SMELT -> BuiltInRegistries.RECIPE_TYPE.getKey(pylon.smeltingType()).toString();
            case PULVERIZE -> CRUSH_PREFIX + "quantity_" + pylon.processingQuantityTier();
            case null, default -> null;
        };
    }

    private static final class Api {
        private static void register() {
            var item = SuperPylonsMod.SUPER_PYLON_ITEM.get();
            var crush = BuiltInRegistries.ITEM.getKey(item).withPath("crushing");
            SccRecipeProviders.claim(crush);
            SccRecipeProviders.register(crush, new SccRecipeProviders.Provider() {
                @Override
                public String stamp() {
                    var server = ServerLifecycleHooks.getCurrentServer();
                    String dataStamp = server != null && server.isSameThread()
                        ? Integer.toHexString(PylonPulverizing.exportRecipes(server.getResourceManager()).hashCode())
                        : PylonCrushRecipes.stamp();
                    return CRUSH_RECIPE_FORMAT + dataStamp;
                }

                @Override
                public void addRecipes(SccRecipeProviders.Output output) {
                    // Keep the original catalyst and IDs available to orders saved before tier-specific planning.
                    output.catalyst(crush, item);
                    for (int tier = 0; tier < QUANTITY_LABELS.length; tier++) {
                        output.catalyst(crush.withPath("crushing/quantity_" + tier), item);
                    }
                    output.catalyst(BuiltInRegistries.RECIPE_TYPE.getKey(RecipeType.SMELTING), item);
                    output.catalyst(BuiltInRegistries.RECIPE_TYPE.getKey(RecipeType.BLASTING), item);
                    output.catalyst(BuiltInRegistries.RECIPE_TYPE.getKey(RecipeType.SMOKING), item);
                    var server = ServerLifecycleHooks.getCurrentServer();
                    var recipes = server != null && server.isSameThread()
                        ? PylonCrushRecipes.recipes(PylonPulverizing.customRecipes(server.getResourceManager()))
                        : PylonCrushRecipes.recipes();
                    for (var recipe : recipes) {
                        int mask = 0;
                        for (int tier : recipe.tiers()) mask |= 1 << tier;
                        var input = BuiltInRegistries.ITEM.getKey(recipe.input().getItem());
                        String id = CRUSH_PREFIX + mask + "/" + input.getNamespace() + "/" + input.getPath();
                        int index = 0;
                        for (var result : recipe.outputs()) {
                            String baseId = id + "/" + index++;
                            for (int tier : recipe.tiers()) {
                                String tierId = tier == recipe.tiers().getFirst() ? baseId : baseId + "/quantity_" + tier;
                                output.recipe(tierId, crush.withPath("crushing/quantity_" + tier))
                                    .name("Pylon Crush " + QUANTITY_LABELS[tier])
                                    .input(recipe.input(), 1).output(result.stack(), result.stack().getCount())
                                    .chance(result.chance() / 100.0F).add();
                            }
                        }
                    }
                }
            });
            SccMachines.register(Api::acceptsStep);
            PylonCrushRecipes.addRefreshListener(SccRecipeProviders::changed);
        }

        private static Boolean acceptsStep(Level level, BlockPos pos, String category, String recipeId) {
            var pylon = pylonAt(level, pos);
            if (pylon == null) return null;
            String active = processingCategory(pylon);
            if (active == null) return false;
            if (pylon.getProcessing() == PylonUpgrade.SMELT) return active.equals(category);
            if (!active.equals(category) && !CRUSH.equals(category)) return false;
            return recipeId == null ? active.equals(category) : acceptsCrushTier(recipeId, pylon.processingQuantityTier());
        }

        private static boolean acceptsCrushTier(String recipeId, int tier) {
            if (recipeId == null || !recipeId.startsWith(CRUSH_PREFIX)) return false;
            int end = recipeId.indexOf('/', CRUSH_PREFIX.length());
            if (end < 0) return false;
            try {
                int mask = Integer.parseInt(recipeId, CRUSH_PREFIX.length(), end, 10);
                return (mask & (1 << tier)) != 0;
            } catch (NumberFormatException error) {
                return false;
            }
        }

        private static boolean usesCalculatorOutput(Level level, BlockPos pylon, BlockPos target) {
            var directed = calculatorAt(level, pylon, target);
            if (directed != null && processingJob(directed)) return true;
            for (Direction side : Direction.values()) {
                BlockPos neighbor = pylon.relative(side);
                if (neighbor.equals(target)) continue;
                var calculator = calculatorAt(level, pylon, neighbor);
                if (calculator != null && (directed == null || processingJob(calculator))) return true;
            }
            return false;
        }

        private static BiFunction<ItemStack, Boolean, ItemStack> itemOutput(Level level, BlockPos pylon, BlockPos target) {
            var calculator = calculatorAt(level, pylon, target);
            if (calculator == null) return null;
            return (stack, simulate) -> {
                if (processingJob(calculator)) return stack;
                var network = calculator.network();
                return network.online() ? SccNetwork.insert(network, stack, simulate) : stack;
            };
        }

        private static boolean processingJob(ProcessingCalculatorBlockEntity calculator) {
            // SCC must pull outputs itself while a job is reserving or waiting on this calculator.
            return calculator.heldJob() != 0 || calculator.awaitingResult();
        }

        private static ProcessingCalculatorBlockEntity calculatorAt(Level level, BlockPos pylon, BlockPos pos) {
            if (!level.hasChunkAt(pos)) return null;
            BlockEntity neighbor = level.getBlockEntity(pos);
            if (neighbor instanceof ProcessingCalculatorBlockEntity calculator && calculator.machinePos().equals(pylon)) {
                return calculator;
            }
            if (neighbor instanceof CableBusBlockEntity cable) {
                for (var part : cable.parts()) {
                    if (part instanceof ProcessingCalculatorBlockEntity calculator && calculator.machinePos().equals(pylon)) {
                        return calculator;
                    }
                }
            }
            return null;
        }
    }
}
