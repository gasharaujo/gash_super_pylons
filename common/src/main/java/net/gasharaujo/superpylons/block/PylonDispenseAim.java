package net.gasharaujo.superpylons.block;

import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

public final class PylonDispenseAim {
    private record Shot(ServerLevel level, Direction direction) {}

    private static final ThreadLocal<Shot> ACTIVE = new ThreadLocal<>();

    private PylonDispenseAim() {}

    static <T> T withDirection(ServerLevel level, Direction direction, Supplier<T> action) {
        Shot previous = ACTIVE.get();
        ACTIVE.set(new Shot(level, direction));
        try {
            return action.get();
        } finally {
            if (previous == null) ACTIVE.remove();
            else ACTIVE.set(previous);
        }
    }

    public static void onSpawn(Level level, Entity entity) {
        Shot shot = ACTIVE.get();
        if (shot == null || shot.level() != level || !(entity instanceof Projectile projectile)) return;
        var direction = shot.direction();
        var speed = projectile.getDeltaMovement().length();
        if (speed == 0) return;
        projectile.setDeltaMovement(direction.getStepX() * speed, direction.getStepY() * speed, direction.getStepZ() * speed);
        projectile.setYRot((float) Math.toDegrees(Math.atan2(direction.getStepX(), direction.getStepZ())));
        projectile.setXRot(direction.getStepY() * 90);
    }
}
