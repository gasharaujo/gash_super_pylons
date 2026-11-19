package net.gasharaujo.superpylons.block;

import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraft.world.item.ItemStack;

final class PylonHubPorts {
    private final PylonHub hub;
    private final IItemHandler items = new Items();
    private final IFluidHandler fluids = new Fluids();
    private final IEnergyStorage energy = new Energy();

    PylonHubPorts(PylonHub hub) { this.hub = hub; }

    IItemHandler items() { return items; }
    IFluidHandler fluids() { return fluids; }
    IEnergyStorage energy() { return energy; }

    private final class Items implements IItemHandler {
        private IItemHandler target(int slot) {
            var pylon = hub.target(slot);
            return pylon == null || pylon.isVoid() ? null : pylon.getItemHandler();
        }

        @Override public int getSlots() { return hub.size(); }
        @Override public ItemStack getStackInSlot(int slot) {
            var handler = target(slot);
            return handler == null ? ItemStack.EMPTY : handler.getStackInSlot(0);
        }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            var handler = target(slot);
            return handler == null ? stack : handler.insertItem(0, stack, simulate);
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            var handler = target(slot);
            return handler == null ? ItemStack.EMPTY : handler.extractItem(0, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) {
            var handler = target(slot);
            return handler == null ? 0 : handler.getSlotLimit(0);
        }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            var handler = target(slot);
            return handler != null && handler.isItemValid(0, stack);
        }
    }

    private final class Fluids implements IFluidHandler {
        private IFluidHandler target(int index) {
            var pylon = hub.target(index);
            return pylon == null || pylon.isVoid() ? null : pylon.getFluidHandler();
        }

        @Override public int getTanks() { return hub.size() * 2; }
        @Override public FluidStack getFluidInTank(int tank) {
            var handler = target(tank / 2);
            return tank >= 0 && handler != null && tank % 2 < handler.getTanks() ?
                handler.getFluidInTank(tank % 2) : FluidStack.EMPTY;
        }
        @Override public int getTankCapacity(int tank) {
            var handler = target(tank / 2);
            return tank >= 0 && handler != null && tank % 2 < handler.getTanks() ?
                handler.getTankCapacity(tank % 2) : 0;
        }
        @Override public boolean isFluidValid(int tank, FluidStack resource) {
            var handler = target(tank / 2);
            return tank >= 0 && handler != null && tank % 2 < handler.getTanks() &&
                handler.isFluidValid(tank % 2, resource);
        }
        @Override public int fill(FluidStack stack, FluidAction action) {
            if (stack.isEmpty()) return 0;
            int filled = 0;
            for (int i = 0; i < hub.size() && filled < stack.getAmount(); i++) {
                var handler = target(i);
                if (handler != null) filled += handler.fill(withAmount(stack, stack.getAmount() - filled), action);
            }
            return filled;
        }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return FluidStack.EMPTY;
            int drained = 0;
            for (int i = 0; i < hub.size() && drained < resource.getAmount(); i++) {
                var handler = target(i);
                if (handler != null) drained += handler.drain(
                    withAmount(resource, resource.getAmount() - drained), action).getAmount();
            }
            return drained == 0 ? FluidStack.EMPTY : withAmount(resource, drained);
        }
        @Override public FluidStack drain(int amount, FluidAction action) {
            if (amount <= 0) return FluidStack.EMPTY;
            for (int i = 0; i < hub.size(); i++) {
                var handler = target(i);
                if (handler == null) continue;
                FluidStack stack = handler.drain(amount, FluidAction.SIMULATE);
                if (!stack.isEmpty()) return drain(withAmount(stack, amount), action);
            }
            return FluidStack.EMPTY;
        }

        private FluidStack withAmount(FluidStack source, int amount) {
            FluidStack copy = source.copy();
            copy.setAmount(amount);
            return copy;
        }
    }

    private final class Energy implements IEnergyStorage {
        private IEnergyStorage target(int index) {
            var pylon = hub.target(index);
            return pylon == null || !pylon.isEnergy() || pylon.isVoid() ? null : pylon.getEnergyHandler(null);
        }

        @Override public int receiveEnergy(int amount, boolean simulate) { return transfer(amount, simulate, true); }
        @Override public int extractEnergy(int amount, boolean simulate) { return transfer(amount, simulate, false); }
        @Override public int getEnergyStored() { return total(false); }
        @Override public int getMaxEnergyStored() { return total(true); }
        @Override public boolean canReceive() { return available(true); }
        @Override public boolean canExtract() { return available(false); }

        private boolean available(boolean insert) {
            for (int i = 0; i < hub.size(); i++) {
                var handler = target(i);
                if (handler != null && (insert ? handler.canReceive() : handler.canExtract())) return true;
            }
            return false;
        }

        private int total(boolean capacity) {
            long total = 0;
            for (int i = 0; i < hub.size() && total < Integer.MAX_VALUE; i++) {
                var handler = target(i);
                if (handler != null) total += capacity ? handler.getMaxEnergyStored() : handler.getEnergyStored();
            }
            return (int) Math.min(Integer.MAX_VALUE, total);
        }

        private int transfer(int amount, boolean simulate, boolean insert) {
            int moved = 0;
            for (int i = 0; i < hub.size() && moved < amount; i++) {
                var handler = target(i);
                if (handler != null) moved += insert ? handler.receiveEnergy(amount - moved, simulate) :
                    handler.extractEnergy(amount - moved, simulate);
            }
            return moved;
        }
    }
}
