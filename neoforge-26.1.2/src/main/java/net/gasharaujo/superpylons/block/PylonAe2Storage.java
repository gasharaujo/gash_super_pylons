package net.gasharaujo.superpylons.block;

import appeng.api.AECapabilities;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
public final class PylonAe2Storage implements MEStorage {
    private final PylonHub hub;

    private PylonAe2Storage(PylonHub hub) { this.hub = hub; }

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(AECapabilities.ME_STORAGE, SuperPylonsMod.SUPER_PYLON_ENTITY.get(),
            (pylon, side) -> pylon.isHub() ? new PylonAe2Storage(pylon.hub()) : null);
    }

    @Override public Component getDescription() {
        return Component.translatable("block.gash_super_pylons.pylon_hub");
    }

    @Override public void getAvailableStacks(KeyCounter out) {
        for (int i = 0; i < hub.size(); i++) {
            var pylon = hub.target(i);
            if (pylon == null || pylon.isVoid()) continue;
            if (pylon.canExtractItems() && pylon.getAvailableItemCount() > 0) {
                add(out, AEItemKey.of(pylon.getStoredItem()), pylon.getAvailableItemCount());
            }
            if (pylon.availableTankAmount() > 0) {
                add(out, AEFluidKey.of(pylon.storedFluidResource()), pylon.availableTankAmount());
            }
            if (pylon.availableXpFluid() > 0) {
                add(out, AEFluidKey.of(FluidResource.of(pylon.preferredXpFluid())), pylon.availableXpFluid());
            }
        }
    }

    private static void add(KeyCounter out, AEKey key, long amount) {
        if (key != null) out.set(key, PylonHub.sum(out.get(key), amount));
    }

    @Override public long insert(AEKey key, long amount, Actionable mode, IActionSource source) {
        return transfer(key, amount, mode.isSimulate(), true);
    }

    @Override public long extract(AEKey key, long amount, Actionable mode, IActionSource source) {
        return transfer(key, amount, mode.isSimulate(), false);
    }

    private long transfer(AEKey key, long amount, boolean simulate, boolean insert) {
        long moved = 0;
        if (key instanceof AEItemKey item) {
            var stack = item.toStack(1);
            for (int i = 0; i < hub.size() && moved < amount; i++) {
                var pylon = hub.target(i);
                if (pylon == null || pylon.isVoid()) continue;
                moved += insert ? pylon.insertItemAmount(stack, amount - moved, simulate) :
                    pylon.extractItemAmount(stack, amount - moved, simulate);
            }
        } else if (key instanceof AEFluidKey fluid) {
            var resource = fluid.toResource();
            for (int i = 0; i < hub.size() && moved < amount; i++) {
                var pylon = hub.target(i);
                if (pylon != null && !pylon.isVoid()) {
                    moved += pylon.exchangeLinkedFluid(resource, amount - moved, insert, simulate);
                }
            }
        }
        return moved;
    }
}
