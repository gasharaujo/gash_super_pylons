package net.gasharaujo.superpylons.block;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class PylonBreaker {
    private static final ThreadLocal<Capture> ACTIVE = new ThreadLocal<>();

    private record Capture(ServerLevel level, BlockPos pos, ServerPlayer player, boolean harvesting, List<ItemEntity> drops) {}
    record Result(List<ItemEntity> drops, ItemStack tool) {}

    private PylonBreaker() {}

    static Result breakBlock(ServerLevel level, BlockPos pos, ServerPlayer player, ItemStack tool) {
        return breakBlock(level, pos, player, tool, false);
    }

    static Result breakTreeBlock(ServerLevel level, BlockPos pos, ServerPlayer player, ItemStack tool) {
        return breakBlock(level, pos, player, tool, true);
    }

    private static Result breakBlock(ServerLevel level, BlockPos pos, ServerPlayer player, ItemStack tool, boolean harvesting) {
        Capture previous = ACTIVE.get();
        Capture capture = new Capture(level, pos, player, harvesting, new ArrayList<>());
        Vec3 previousPosition = player.position();
        ItemStack previousHand = player.getMainHandItem();
        var at = Vec3.atCenterOf(pos.above());
        player.setPos(at.x, at.y, at.z);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        ACTIVE.set(capture);
        try {
            if (!player.gameMode.destroyBlock(pos)) return new Result(List.of(), player.getMainHandItem());
            var drops = capture.drops().stream()
                .filter(drop -> drop.isAlive() && !drop.getItem().isEmpty() && level.getEntity(drop.getId()) == drop).toList();
            return new Result(drops, player.getMainHandItem());
        } finally {
            if (previous == null) ACTIVE.remove();
            else ACTIVE.set(previous);
            player.setItemInHand(InteractionHand.MAIN_HAND, previousHand);
            player.setPos(previousPosition.x, previousPosition.y, previousPosition.z);
        }
    }

    public static boolean isHarvesting(Player player, BlockPos pos) {
        Capture capture = ACTIVE.get();
        return capture != null && capture.harvesting() && capture.player() == player && capture.pos().equals(pos);
    }

    public static void trackDrop(Level level, Entity entity) {
        if (!(entity instanceof ItemEntity item)) return;
        Capture capture = ACTIVE.get();
        if (capture != null && capture.level() == level && capture.pos().equals(item.blockPosition())) {
            capture.drops().add(item);
        }
    }
}
