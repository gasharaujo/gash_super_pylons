package net.gasharaujo.superpylons.compat;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class PylonAgriCraft {
    private static final Method HARVEST = findHarvest();

    private PylonAgriCraft() {}

    private static Method findHarvest() {
        try {
            Class<?> crop = Class.forName("com.agricraft.agricraft.api.crop.AgriCrop");
            return crop.getMethod("harvest", Consumer.class, LivingEntity.class);
        } catch (ClassNotFoundException ignored) {
            return null;
        } catch (ReflectiveOperationException | LinkageError error) {
            LogUtils.getLogger().warn("Could not load the optional AgriCraft harvest API", error);
            return null;
        }
    }

    public static boolean isCrop(BlockEntity entity) {
        return HARVEST != null && HARVEST.getDeclaringClass().isInstance(entity);
    }

    public static List<ItemStack> harvest(BlockEntity crop) {
        List<ItemStack> drops = new ArrayList<>();
        Consumer<ItemStack> collect = stack -> {
            if (!stack.isEmpty()) drops.add(stack.copy());
        };
        try {
            // AgriCraft accepts a null harvester for automation and handles maturity and regrowth itself.
            return Boolean.TRUE.equals(HARVEST.invoke(crop, collect, null)) ? drops : List.of();
        } catch (ReflectiveOperationException error) {
            LogUtils.getLogger().warn("AgriCraft harvest failed at {}", crop.getBlockPos(), error);
            return List.of();
        }
    }
}
