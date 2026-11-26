package net.gasharaujo.superpylons.compat;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.gasharaujo.superpylons.block.PylonBreaker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

public final class PylonTreeChop {
    private static Method originalState;

    private PylonTreeChop() {}

    public static void register(BiConsumer<Class<?>, Predicate<Object>> listen) {
        try {
            // TreeChop keeps these names across Forge, NeoForge and the 26.3 continuation.
            Class<?> startChop = Class.forName("ht.treechop.api.ChopEvent$StartChopEvent");
            Method player = startChop.getMethod("getPlayer");
            Method pos = startChop.getMethod("getChoppedBlockPos");
            Method original = Class.forName("ht.treechop.common.block.ChoppedLogBlock$MyEntity").getMethod("getOriginalState");
            listen.accept(startChop, event -> isHarvesterChop(event, player, pos));
            originalState = original;
        } catch (ReflectiveOperationException | LinkageError | ClassCastException error) {
            LogUtils.getLogger().warn("Could not register TreeChop compatibility", error);
        }
    }

    private static boolean isHarvesterChop(Object event, Method player, Method pos) {
        try {
            return PylonBreaker.isHarvesting((Player) player.invoke(event), (BlockPos) pos.invoke(event));
        } catch (ReflectiveOperationException error) {
            LogUtils.getLogger().warn("Could not read TreeChop's chop event", error);
            return false;
        }
    }

    public static BlockState originalState(ServerLevel level, BlockPos pos, BlockState state) {
        if (originalState == null || !state.hasBlockEntity()) return state;
        var blockEntity = level.getBlockEntity(pos);
        if (!originalState.getDeclaringClass().isInstance(blockEntity)) return state;
        try {
            return (BlockState) originalState.invoke(blockEntity);
        } catch (ReflectiveOperationException error) {
            LogUtils.getLogger().warn("Could not read TreeChop's original log at {}", pos, error);
            return state;
        }
    }
}
