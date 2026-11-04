package net.gasharaujo.superpylons;

import net.gasharaujo.superpylons.block.SuperPylonBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = SuperPylonsMod.MOD_ID)
public final class PylonInteractions {
    private PylonInteractions() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void removeUpgrade(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (event.getHand() != InteractionHand.MAIN_HAND || !player.isShiftKeyDown() ||
            !player.getMainHandItem().isEmpty() || player.getOffhandItem().isEmpty() ||
            event.getUseBlock().isFalse()) return;
        // Vanilla skips the block-use method while sneaking with an occupied offhand.
        if (event.getLevel().getBlockEntity(event.getPos()) instanceof SuperPylonBlockEntity pylon &&
            pylon.use(player, InteractionHand.MAIN_HAND)) {
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
            event.setCanceled(true);
        }
    }

    /** Creative skips Block.attack, so intercept it while the pylon still holds items. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void creativeAttack(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (!player.getAbilities().instabuild ||
            player.getMainHandItem().is(ItemTags.PICKAXES) ||
            event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;
        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof SuperPylonBlockEntity pylon) ||
            pylon.getStoredCount() == 0) return;
        if (event.getLevel().isClientSide()) {
            event.setCanceled(true);
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer) ||
            !(event.getLevel() instanceof ServerLevel server) ||
            !player.getAbilities().mayBuild || !server.mayInteract(player, event.getPos()) ||
            !server.getWorldBorder().isWithinBounds(event.getPos()) ||
            player.getEyePosition().distanceToSqr(Vec3.atCenterOf(event.getPos())) > 36.0 ||
            server.getServer().isUnderSpawnProtection(server, event.getPos(), serverPlayer)) return;
        pylon.withdraw(player);
        event.setCanceled(true);
    }
}
