package net.gasharaujo.superpylons.block;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

final class PylonHubPorts {
    private final PylonHub hub;
    private final ResourceHandler<ItemResource> items = new LinkedResources<>(ItemResource.EMPTY, 1) {
        @Override ResourceHandler<ItemResource> port(SuperPylonBlockEntity target) { return target.getItemHandler(); }
    };
    private final ResourceHandler<FluidResource> fluids = new LinkedResources<>(FluidResource.EMPTY, 2) {
        @Override ResourceHandler<FluidResource> port(SuperPylonBlockEntity target) { return target.getFluidHandler(); }
    };
    private final EnergyHandler energy = new LinkedEnergy();

    PylonHubPorts(PylonHub hub) { this.hub = hub; }

    ResourceHandler<ItemResource> items() { return items; }
    ResourceHandler<FluidResource> fluids() { return fluids; }
    EnergyHandler energy() { return energy; }

    private abstract class LinkedResources<T extends Resource> implements ResourceHandler<T> {
        private final T empty;
        private final int slotsPerPylon;

        LinkedResources(T empty, int slotsPerPylon) {
            this.empty = empty;
            this.slotsPerPylon = slotsPerPylon;
        }

        abstract ResourceHandler<T> port(SuperPylonBlockEntity target);

        private ResourceHandler<T> target(int index) {
            if (index < 0) return null;
            var pylon = hub.target(index / slotsPerPylon);
            if (pylon == null || pylon.isVoid()) return null;
            var handler = port(pylon);
            return handler == null || slot(index) >= handler.size() ? null : handler;
        }

        private int slot(int index) { return index % slotsPerPylon; }

        @Override public int size() { return hub.size() * slotsPerPylon; }
        @Override public T getResource(int index) {
            var handler = target(index);
            return handler == null ? empty : handler.getResource(slot(index));
        }
        @Override public long getAmountAsLong(int index) {
            var handler = target(index);
            return handler == null ? 0 : handler.getAmountAsLong(slot(index));
        }
        @Override public long getCapacityAsLong(int index, T resource) {
            var handler = target(index);
            return handler == null ? 0 : handler.getCapacityAsLong(slot(index), resource);
        }
        @Override public boolean isValid(int index, T resource) {
            var handler = target(index);
            return handler != null && handler.isValid(slot(index), resource);
        }
        @Override public int insert(int index, T resource, int amount, TransactionContext transaction) {
            var handler = target(index);
            return handler == null ? 0 : handler.insert(slot(index), resource, amount, transaction);
        }
        @Override public int extract(int index, T resource, int amount, TransactionContext transaction) {
            var handler = target(index);
            return handler == null ? 0 : handler.extract(slot(index), resource, amount, transaction);
        }
    }

    private final class LinkedEnergy implements EnergyHandler {
        private EnergyHandler target(int index) {
            var pylon = hub.target(index);
            return pylon == null || !pylon.isEnergy() || pylon.isVoid() ? null : pylon.getEnergyHandler(null);
        }

        @Override public long getAmountAsLong() { return total(false); }
        @Override public long getCapacityAsLong() { return total(true); }
        @Override public int insert(int amount, TransactionContext transaction) { return move(amount, transaction, true); }
        @Override public int extract(int amount, TransactionContext transaction) { return move(amount, transaction, false); }

        private long total(boolean capacity) {
            long total = 0;
            for (int i = 0; i < hub.size(); i++) {
                var handler = target(i);
                if (handler != null) total = PylonHub.sum(total,
                    capacity ? handler.getCapacityAsLong() : handler.getAmountAsLong());
            }
            return total;
        }

        private int move(int amount, TransactionContext transaction, boolean insert) {
            TransferPreconditions.checkNonNegative(amount);
            int moved = 0;
            for (int i = 0; i < hub.size() && moved < amount; i++) {
                var handler = target(i);
                if (handler != null) moved += insert ? handler.insert(amount - moved, transaction) :
                    handler.extract(amount - moved, transaction);
            }
            return moved;
        }
    }
}
