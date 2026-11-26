package net.gasharaujo.superpylons.compat;

import com.mojang.logging.LogUtils;
import de.scc.calculator.api.SccMachines;
import de.scc.calculator.api.SccRecipeProviders;
import de.scc.calculator.network.CableBusBlockEntity;
import de.scc.calculator.network.ProcessingCalculatorBlockEntity;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.gasharaujo.superpylons.block.PylonPulverizing;
import net.gasharaujo.superpylons.block.SuperPylonBlockEntity;
import net.gasharaujo.superpylons.client.PylonCrushRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

public final class PylonSccCompat {
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

    public static boolean hasCalculator(Level level, BlockPos pylon) {
        return registered && Api.hasCalculator(level, pylon);
    }

    private static final class Api {
        private static final String CRUSH = SuperPylonsMod.MOD_ID + ":crushing";
        private static final String CRUSH_PREFIX = CRUSH + "/";

        private static void register() {
            var item = SuperPylonsMod.SUPER_PYLON_ITEM.get();
            var crush = BuiltInRegistries.ITEM.getKey(item).withPath("crushing");
            SccRecipeProviders.claim(crush);
            SccRecipeProviders.register(crush, new SccRecipeProviders.Provider() {
                @Override
                public String stamp() {
                    var server = ServerLifecycleHooks.getCurrentServer();
                    if (server == null || !server.isSameThread()) return PylonCrushRecipes.stamp();
                    return Integer.toHexString(PylonPulverizing.exportRecipes(server.getResourceManager()).hashCode());
                }

                @Override
                public void addRecipes(SccRecipeProviders.Output output) {
                    output.catalyst(crush, item);
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
                            output.recipe(id + "/" + index++, crush).name("Pylon Crush")
                                .input(recipe.input(), 1).output(result.stack(), result.stack().getCount())
                                .chance(result.chance() / 100.0F).add();
                        }
                    }
                }
            });
            SccMachines.register(Api::acceptsStep);
            PylonCrushRecipes.addRefreshListener(SccRecipeProviders::changed);
        }

        private static Boolean acceptsStep(Level level, BlockPos pos, String category, String recipeId) {
            if (!(level.getBlockEntity(pos) instanceof SuperPylonBlockEntity pylon)) return null;
            if (!pylon.canUseProcessingCalculator()) return false;
            return switch (pylon.getProcessing()) {
                case SMELT -> BuiltInRegistries.RECIPE_TYPE.getKey(pylon.smeltingType()).toString().equals(category);
                case PULVERIZE -> CRUSH.equals(category) && acceptsCrushTier(recipeId, pylon.processingQuantityTier());
                case null, default -> false;
            };
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

        private static boolean hasCalculator(Level level, BlockPos pylon) {
            for (Direction side : Direction.values()) {
                BlockPos pos = pylon.relative(side);
                if (!level.hasChunkAt(pos)) continue;
                BlockEntity neighbor = level.getBlockEntity(pos);
                if (targetsPylon(neighbor, pylon)) return true;
                if (neighbor instanceof CableBusBlockEntity cable) {
                    for (var part : cable.parts()) {
                        if (targetsPylon(part, pylon)) return true;
                    }
                }
            }
            return false;
        }

        private static boolean targetsPylon(BlockEntity entity, BlockPos pylon) {
            return entity instanceof ProcessingCalculatorBlockEntity calculator && calculator.machinePos().equals(pylon);
        }
    }
}
