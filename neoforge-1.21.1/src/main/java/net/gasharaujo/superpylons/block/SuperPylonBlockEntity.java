package net.gasharaujo.superpylons.block;

import com.mojang.authlib.GameProfile;
import java.util.function.BiFunction;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public final class SuperPylonBlockEntity extends PylonBlockEntityBase {
    private final PylonTank<FluidStack> tank = new PylonTank<>();
    private final TankHandler fluidHandler = new TankHandler();
    private final IEnergyStorage energyInput = new EnergyPort(Direction.UP);
    private final IEnergyStorage energyOutput = new EnergyPort(Direction.DOWN);
    private final IEnergyStorage energyInternal = new EnergyPort(null);

    public IFluidHandler getFluidHandler() {
        return isTank() ? fluidHandler : null;
    }

    public SuperPylonBlockEntity(BlockPos pos, BlockState state) {
        super(SuperPylonsMod.SUPER_PYLON_ENTITY.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("upgrades", saveUpgrades());
        if (getExperience() > 0) tag.putLong("experience", getExperience());
        if (getEnergy() > 0) tag.putInt("energy", getEnergy());
        if (!harvestBuffer.isEmpty()) {
            ListTag harvest = new ListTag();
            for (ItemStack stack : harvestBuffer) harvest.add(stack.save(registries));
            tag.put("harvest", harvest);
        }
        tag.putLong("stored_count", getStoredCount());
        tag.putLong("fluid_amount", tank.amount());
        if (getStoredCount() > 0) tag.put("stored_item", getStoredItem().save(registries));
        if (tank.amount() > 0) tag.put("fluid_template", tank.resource().save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadUpgrades(tag.getString("upgrades"));
        loadSpecialtyAmounts(tag.getLong("experience"), tag.getInt("energy"));
        harvestBuffer.clear();
        ListTag harvest = tag.getList("harvest", Tag.TAG_COMPOUND);
        for (int i = 0; i < harvest.size(); i++) {
            ItemStack stack = ItemStack.parseOptional(registries, harvest.getCompound(i));
            if (!stack.isEmpty()) harvestBuffer.add(stack);
        }
        contents.restore(ItemStack.parseOptional(registries, tag.getCompound("stored_item")), tag.getLong("stored_count"), getItemCapacity());
        FluidStack resource = FluidStack.parseOptional(registries, tag.getCompound("fluid_template"));
        tank.restore(new PylonTank.State<>(resource.isEmpty() || !canFillFluid(resource.getFluid()) ? null : resource,
            Math.min(getFluidCapacity(), tag.getLong("fluid_amount"))));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override public long getFluidAmount() { return tank.amount(); }

    @Override
    public Component getFluidName() {
        return tank.amount() == 0 ? Component.empty() : tank.resource().getHoverName();
    }

    @Override protected void clearFluid() { tank.clear(); }

    @Override
    protected void generateFluid(Fluid fluid, int amount) {
        fluidHandler.fill(new FluidStack(fluid, amount), IFluidHandler.FluidAction.EXECUTE);
    }

    @Override
    protected void interactWithTank(Player player, InteractionHand hand) {
        FluidUtil.interactWithFluidHandler(player, hand, fluidHandler);
    }

    @Override
    protected void capabilitiesChanged() {
        if (level != null) level.invalidateCapabilities(worldPosition);
    }

    @Override
    protected ServerPlayer getAutomationPlayer(ServerLevel server, GameProfile profile) {
        return FakePlayerFactory.get(server, profile);
    }

    @Override
    protected ItemStack createPackedDrop() {
        ItemStack stack = new ItemStack(SuperPylonsMod.SUPER_PYLON_ITEM.get());
        saveToItem(stack, level.registryAccess());
        return stack;
    }

    @Override
    protected void notifyPlayer(Player player, Component message) {
        player.displayClientMessage(message, true);
    }

    public IEnergyStorage getEnergyHandler(Direction side) {
        if (!isEnergy()) return null;
        if (side == null) return energyInternal;
        if (side == Direction.UP) return energyInput;
        return side == Direction.DOWN ? energyOutput : null;
    }

    @Override
    protected long orbExperience(ExperienceOrb orb) {
        return (long) orb.getValue() * Math.max(1, orb.saveWithoutId(new CompoundTag()).getInt("Count"));
    }

    @Override
    protected BiFunction<ItemStack, Boolean, ItemStack> harvestOutput(ServerLevel server, BlockPos target) {
        IItemHandler handler = server.getCapability(Capabilities.ItemHandler.BLOCK, target, Direction.UP);
        return handler == null ? null : (stack, simulate) -> ItemHandlerHelper.insertItemStacked(handler, stack, simulate);
    }

    @Override
    protected void transferEnergy(ServerLevel server) {
        int inputLimit = energyLimit(true, Integer.MAX_VALUE);
        if (inputLimit > 0) {
            IEnergyStorage source = energyAt(server, worldPosition.above(), Direction.DOWN);
            if (source != null) exchangeEnergy(true, source.extractEnergy(inputLimit, false), false);
        }
        int outputLimit = energyLimit(false, Integer.MAX_VALUE);
        if (outputLimit > 0 && getStoredCount() == 1) {
            ItemStack stack = getStoredItem();
            IEnergyStorage item = stack.getCapability(Capabilities.EnergyStorage.ITEM);
            if (item != null) {
                int accepted = item.receiveEnergy(outputLimit, false);
                if (accepted > 0) {
                    exchangeEnergy(false, accepted, false);
                    contents.restore(stack, 1, getItemCapacity());
                }
            }
        }
        outputLimit = energyLimit(false, Integer.MAX_VALUE);
        if (outputLimit > 0) {
            IEnergyStorage target = energyAt(server, worldPosition.below(), Direction.UP);
            if (target != null) exchangeEnergy(false, target.receiveEnergy(outputLimit, false), false);
        }
    }

    private static IEnergyStorage energyAt(ServerLevel server, BlockPos pos, Direction side) {
        return server.hasChunkAt(pos) ? server.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side) : null;
    }

    private final class EnergyPort implements IEnergyStorage {
        private final Direction side;

        private EnergyPort(Direction side) { this.side = side; }

        @Override public boolean canReceive() { return isEnergy() && side != Direction.DOWN; }
        @Override public boolean canExtract() { return isEnergy() && side != Direction.UP; }
        @Override public int getEnergyStored() { return isEnergy() ? getEnergy() : 0; }
        @Override public int getMaxEnergyStored() { return getEnergyCapacity(); }

        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            return canReceive() ? exchangeEnergy(true, amount, simulate) : 0;
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            return canExtract() ? exchangeEnergy(false, amount, simulate) : 0;
        }
    }

    private int exchangeEnergy(boolean input, int requested, boolean simulate) {
        int moved = energyLimit(input, requested);
        if (moved > 0 && !simulate) {
            energy.transfer(input, moved, level.getGameTime());
            energyChanged();
        }
        return moved;
    }

    private final class TankHandler implements IFluidHandler {
        @Override public int getTanks() { return 1; }

        @Override
        public FluidStack getFluidInTank(int index) {
            if (index != 0 || !isTank() || tank.amount() == 0) return FluidStack.EMPTY;
            FluidStack stack = tank.resource().copy();
            stack.setAmount((int) Math.min(Integer.MAX_VALUE, tank.amount()));
            return stack;
        }

        @Override public int getTankCapacity(int index) {
            return index == 0 ? (int) Math.min(Integer.MAX_VALUE, getFluidCapacity()) : 0;
        }

        @Override
        public boolean isFluidValid(int index, FluidStack resource) {
            return index == 0 && !resource.isEmpty() && canFillFluid(resource.getFluid());
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!isFluidValid(0, resource) || isRemoved()) return 0;
            FluidStack template = resource.copy();
            template.setAmount(1);
            int filled = tank.insert(template, resource.getAmount(), getFluidCapacity(), FluidStack::isSameFluidSameComponents, action.simulate());
            if (filled > 0 && action.execute()) sync();
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || tank.amount() == 0 ||
                !FluidStack.isSameFluidSameComponents(tank.resource(), resource)) return FluidStack.EMPTY;
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maximum, FluidAction action) {
            if (!isTank() || isRemoved() || tank.amount() == 0 || maximum <= 0) return FluidStack.EMPTY;
            FluidStack result = tank.resource().copy();
            result.setAmount(tank.extract(maximum, action.simulate()));
            if (action.execute()) sync();
            return result;
        }
    }

}
