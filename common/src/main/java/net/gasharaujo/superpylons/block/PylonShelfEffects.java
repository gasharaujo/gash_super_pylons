package net.gasharaujo.superpylons.block;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.EntityBlock;

/** Optional bridge: all shelf values and enchanting rolls come from Apotheosis itself. */
final class PylonShelfEffects {
    record Selection(List<EnchantmentInstance> selected, List<EnchantmentInstance> available, int power) {}
    private static boolean loggedFailure;

    private PylonShelfEffects() {}

    static boolean isShelf(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem)) return false;
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return (id.getNamespace().equals("apotheosis") || id.getNamespace().equals("apothic_enchanting")) &&
            id.getPath().contains("shelf");
    }

    @SuppressWarnings("unchecked")
    static Selection select(ServerLevel level, BlockPos pos, ItemStack item, int basePower,
                            List<ItemStack> shelves, Collection<?> blacklist, Object enchantmentRegistry) {
        if (shelves.isEmpty()) return null;
        try {
            boolean legacy = enchantmentRegistry == null;
            String root = legacy ? "dev.shadowsoffire.apotheosis.ench." : "dev.shadowsoffire.apothic_enchanting.";
            Class<?> statsClass = Class.forName(root + (legacy ? "table.ApothEnchantmentMenu$TableStats" : "table.EnchantmentTableStats"));
            Class<?> builderClass = Class.forName(statsClass.getName() + "$Builder");
            Object builder = legacy ? builderClass.getConstructor(int.class).newInstance(call(item, "getEnchantmentValue")) :
                builderClass.getConstructor().newInstance();
            Class<?> registry = Class.forName(root + "table.EnchantingStatRegistry");
            Class<?> statBlock = Class.forName(root + (legacy ? "api.IEnchantingBlock" : "api.EnchantmentStatBlock"));
            // Add the pylon's base power after the shelves' capped contributions have been collected.
            call(builder, "addEterna", basePower / 2F, 100F);
            for (ItemStack shelf : shelves) {
                var state = ((BlockItem) shelf.getItem()).getBlock().defaultBlockState();
                Object block = state.getBlock();
                Object reader = level;
                if (!legacy) {
                    var entity = block instanceof EntityBlock entityBlock ? entityBlock.newBlockEntity(pos, state) : null;
                    if (entity != null) {
                        entity.setLevel(level);
                        call(entity, "applyComponentsFromItemStack", shelf);
                    }
                    reader = Proxy.newProxyInstance(LevelReader.class.getClassLoader(), new Class<?>[]{LevelReader.class},
                        (proxy, method, arguments) -> {
                            if (arguments != null && arguments.length > 0 && pos.equals(arguments[0])) {
                                if (method.getName().equals("getBlockState")) return state;
                                if (method.getName().equals("getFluidState")) return state.getFluidState();
                                if (method.getName().equals("getBlockEntity") && arguments.length == 1) return entity;
                            }
                            return method.invoke(level, arguments);
                        });
                }
                call(builder, "addEterna", call(registry, "getEterna", state, reader, pos),
                    call(registry, "getMaxEterna", state, reader, pos));
                call(builder, "addQuanta", call(registry, "getQuanta", state, reader, pos));
                call(builder, "addArcana", call(registry, "getArcana", state, reader, pos));
                call(builder, "addClues", call(registry, "getBonusClues", state, reader, pos));
                if (legacy) call(builder, "addRectification", call(registry, "getQuantaRectification", state, level, pos));
                if (statBlock.isInstance(block)) {
                    Collection<?> excluded = (Collection<?>) call(block, "getBlacklistedEnchantments", state, reader, pos);
                    for (Object enchantment : excluded) call(builder, "blacklistEnchant", enchantment);
                    if (Boolean.TRUE.equals(call(block, "allowsTreasure", state, reader, pos))) call(builder, "setAllowsTreasure", true);
                    if (!legacy && Boolean.TRUE.equals(call(block, "providesStability", state, reader, pos))) call(builder, "setStable", true);
                }
            }
            for (Object enchantment : blacklist) call(builder, "blacklistEnchant", enchantment);
            Object stats = call(builder, "build");
            int power = Math.max(1, Math.round(((Number) call(stats, "eterna")).floatValue() * 2));
            Class<?> helper = Class.forName(root + "table." + (legacy ? "RealEnchantmentHelper" : "ApothEnchantmentHelper"));
            List<EnchantmentInstance> chosen;
            List<EnchantmentInstance> available;
            if (legacy) {
                chosen = (List<EnchantmentInstance>) call(helper, "selectEnchantment", level.getRandom(), item, power,
                    call(stats, "quanta"), call(stats, "arcana"), call(stats, "rectification"),
                    call(stats, "treasure"), call(stats, "blacklist"));
                available = (List<EnchantmentInstance>) call(helper, "getAvailableEnchantmentResults", power, item,
                    call(stats, "treasure"), call(stats, "blacklist"));
            } else {
                chosen = (List<EnchantmentInstance>) call(helper, "selectEnchantment", level.getRandom(), item, power, stats, enchantmentRegistry);
                Stream<?> possible = (Stream<?>) call(helper, "getPossibleEnchantments", enchantmentRegistry, item, stats);
                try (possible) {
                    available = (List<EnchantmentInstance>) call(helper, "getAvailableEnchantmentResults", power, item, possible);
                }
            }
            return new Selection(chosen, available, power);
        } catch (ReflectiveOperationException | RuntimeException error) {
            if (!loggedFailure) {
                loggedFailure = true;
                LogUtils.getLogger().error("Cannot apply installed enchanting shelves; keeping pylon resources unchanged", error);
            }
            return new Selection(List.of(), List.of(), basePower);
        }
    }

    private static Object call(Object receiver, String name, Object... args) throws ReflectiveOperationException {
        Class<?> type = receiver instanceof Class<?> cls ? cls : receiver.getClass();
        for (Method method : type.getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != args.length) continue;
            Class<?>[] parameters = method.getParameterTypes();
            boolean matches = true;
            for (int i = 0; i < args.length; i++) {
                Class<?> expected = parameters[i];
                if (expected == int.class) expected = Integer.class;
                if (expected == float.class) expected = Float.class;
                if (expected == boolean.class) expected = Boolean.class;
                if (args[i] != null && !expected.isInstance(args[i])) { matches = false; break; }
            }
            if (matches) return method.invoke(receiver instanceof Class<?> ? null : receiver, args);
        }
        throw new NoSuchMethodException(type.getName() + "." + name);
    }
}
