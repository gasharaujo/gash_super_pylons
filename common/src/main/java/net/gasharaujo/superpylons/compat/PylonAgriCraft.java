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
    private record CropApi(Class<?> cropType, Method hasPlant, Method harvest) {}

    private static final CropApi API = findApi();

    private PylonAgriCraft() {}

    private static CropApi findApi() {
        try {
            Class<?> crop = Class.forName("com.agricraft.agricraft.api.crop.AgriCrop");
            return new CropApi(crop, crop.getMethod("hasPlant"),
                crop.getMethod("harvest", Consumer.class, LivingEntity.class));
        } catch (ClassNotFoundException ignored) {
            return null;
        } catch (ReflectiveOperationException | LinkageError error) {
            LogUtils.getLogger().warn("Could not load the optional AgriCraft crop API", error);
            return null;
        }
    }

    public static boolean isCrop(BlockEntity entity) {
        return API != null && API.cropType().isInstance(entity);
    }

    public static boolean hasPlant(BlockEntity entity) {
        if (!isCrop(entity)) return false;
        try {
            return Boolean.TRUE.equals(API.hasPlant().invoke(entity));
        } catch (ReflectiveOperationException error) {
            LogUtils.getLogger().warn("Could not check for an AgriCraft plant at {}", entity.getBlockPos(), error);
            return false;
        }
    }

    public static List<ItemStack> harvest(BlockEntity crop) {
        List<ItemStack> drops = new ArrayList<>();
        Consumer<ItemStack> collect = stack -> {
            if (!stack.isEmpty()) drops.add(stack.copy());
        };
        try {
            // AgriCraft accepts a null harvester for automation and handles maturity and regrowth itself.
            return Boolean.TRUE.equals(API.harvest().invoke(crop, collect, null)) ? drops : List.of();
        } catch (ReflectiveOperationException error) {
            LogUtils.getLogger().warn("AgriCraft harvest failed at {}", crop.getBlockPos(), error);
            return List.of();
        }
    }
}
