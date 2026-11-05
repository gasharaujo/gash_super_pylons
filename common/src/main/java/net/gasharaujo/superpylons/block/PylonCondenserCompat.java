package net.gasharaujo.superpylons.block;

import java.lang.reflect.Method;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Uses AE2's long-quantity storage API for bulk, without requiring AE2 to be installed. */
final class PylonCondenserCompat {
    private PylonCondenserCompat() {}

    static Port port(BlockEntity target, boolean fluid, Object resource) {
        if (target == null || resource == null || !target.getClass().getName().equals("appeng.blockentity.misc.CondenserBlockEntity")) return null;
        try {
            Class<?> keyType = Class.forName("appeng.api.stacks." + (fluid ? "AEFluidKey" : "AEItemKey"));
            Object key = null;
            for (Method method : keyType.getMethods()) {
                if (method.getName().equals("of") && method.getParameterCount() == 1 &&
                    method.getParameterTypes()[0].isInstance(resource)) {
                    key = method.invoke(null, resource);
                    break;
                }
            }
            if (key == null) return null;
            Class<?> mode = Class.forName("appeng.api.config.Actionable");
            Class<?> actionSource = Class.forName("appeng.api.networking.security.IActionSource");
            Object source = actionSource.getMethod("empty").invoke(null);
            Object storage = target.getClass().getMethod("getMEStorage").invoke(target);
            Method insert = Class.forName("appeng.api.storage.MEStorage").getMethod("insert",
                Class.forName("appeng.api.stacks.AEKey"), long.class, mode, actionSource);
            return new Port(storage, key, source, mode.getField("MODULATE").get(null), insert);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    record Port(Object storage, Object key, Object source, Object mode, Method insert) {
        long insert(long amount) {
            if (amount <= 0) return 0;
            try {
                return Math.max(0, Math.min(amount, ((Number) insert.invoke(storage, key, amount, mode, source)).longValue()));
            } catch (ReflectiveOperationException failure) {
                // Do not retry a possibly committed insertion through a different API.
                throw new IllegalStateException("AE2 bulk insertion failed", failure);
            }
        }
    }
}
