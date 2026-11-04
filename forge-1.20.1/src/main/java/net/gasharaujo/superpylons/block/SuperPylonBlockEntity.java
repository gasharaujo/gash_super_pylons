package net.gasharaujo.superpylons.block;

import com.mojang.authlib.GameProfile;
import java.util.function.BiFunction;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

public final class SuperPylonBlockEntity extends PylonBlockEntityBase {
    private final PylonTank<FluidStack> tank = new PylonTank<>();
    private final TankHandler fluidHandler = new TankHandler();
    private final EnergyPort energyInput = new EnergyPort(Direction.UP);
    private final EnergyPort energyOutput = new EnergyPort(Direction.DOWN);
    private final EnergyPort energyInternal = new EnergyPort(null);
    private LazyOptional<IEnergyStorage> inputCapability = LazyOptional.of(() -> energyInput);
    private LazyOptional<IEnergyStorage> outputCapability = LazyOptional.of(() -> energyOutput);
    private LazyOptional<IEnergyStorage> internalCapability = LazyOptional.of(() -> energyInternal);
    private LazyOptional<IFluidHandler> tankCapability = LazyOptional.of(() -> fluidHandler);

    public IFluidHandler getFluidHandler() {
        return isTank() ? fluidHandler : null;
    }

    public SuperPylonBlockEntity(BlockPos pos, BlockState state) {
        super(SuperPylonsMod.SUPER_PYLON_ENTITY.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("upgrades", saveUpgrades());
        if (getExperience() > 0) tag.putLong("experience", getExperience());
        if (getEnergy() > 0) tag.putInt("energy", getEnergy());
        if (!harvestBuffer.isEmpty()) {
            ListTag harvest = new ListTag();
            for (ItemStack stack : harvestBuffer) harvest.add(stack.save(new CompoundTag()));
            tag.put("harvest", harvest);
        }
        tag.putLong("stored_count", getStoredCount());
        tag.putLong("fluid_amount", tank.amount());
        if (getStoredCount() > 0) tag.put("stored_item", getStoredItem().save(new CompoundTag()));
        if (tank.amount() > 0) tag.put("fluid_template", tank.resource().writeToNBT(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        loadUpgrades(tag.getString("upgrades"));
        loadSpecialtyAmounts(tag.getLong("experience"), tag.getInt("energy"));
        harvestBuffer.clear();
        ListTag harvest = tag.getList("harvest", Tag.TAG_COMPOUND);
        for (int i = 0; i < harvest.size(); i++) {
            ItemStack stack = ItemStack.of(harvest.getCompound(i));
            if (!stack.isEmpty()) harvestBuffer.add(stack);
        }
        contents.restore(ItemStack.of(tag.getCompound("stored_item")), tag.getLong("stored_count"), getItemCapacity());
        FluidStack resource = FluidStack.loadFluidStackFromNBT(tag.getCompound("fluid_template"));
        tank.restore(new PylonTank.State<>(resource.isEmpty() || !canFillFluid(resource.getFluid()) ? null : resource,
            tag.getLong("fluid_amount")));
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override public long getFluidAmount() { return tank.amount(); }

    @Override
    public Component getFluidName() {
        return tank.amount() == 0 ? Component.empty() : tank.resource().getDisplayName();
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
        invalidatePylonCapabilities();
        createPylonCapabilities();
    }

    @Override
    protected ServerPlayer getAutomationPlayer(ServerLevel server, GameProfile profile) {
        return FakePlayerFactory.get(server, profile);
    }

    @Override
    protected ItemStack createPackedDrop() {
        ItemStack stack = new ItemStack(SuperPylonsMod.SUPER_PYLON_ITEM.get());
        stack.addTagElement("BlockEntityTag", getUpdateTag());
        return stack;
    }

    @Override
    protected void notifyPlayer(Player player, Component message) {
        player.displayClientMessage(message, true);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER && isTank() && !isRemoved()) return tankCapability.cast();
        if (cap == ForgeCapabilities.ENERGY && isEnergy() && !isRemoved()) {
            if (side == null) return internalCapability.cast();
            if (side == Direction.UP) return inputCapability.cast();
            if (side == Direction.DOWN) return outputCapability.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        invalidatePylonCapabilities();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        createPylonCapabilities();
    }

    private void invalidatePylonCapabilities() {
        tankCapability.invalidate();
        inputCapability.invalidate();
        outputCapability.invalidate();
        internalCapability.invalidate();
    }

    private void createPylonCapabilities() {
        tankCapability = LazyOptional.of(() -> fluidHandler);
        inputCapability = LazyOptional.of(() -> energyInput);
        outputCapability = LazyOptional.of(() -> energyOutput);
        internalCapability = LazyOptional.of(() -> energyInternal);
    }

    @Override
    protected long orbExperience(ExperienceOrb orb) {
        return (long) orb.getValue() * Math.max(1, orb.saveWithoutId(new CompoundTag()).getInt("Count"));
    }

    @Override
    protected BiFunction<ItemStack, Boolean, ItemStack> harvestOutput(ServerLevel server, BlockPos target) {
        var entity = server.getBlockEntity(target);
        IItemHandler handler = entity == null ? null : entity.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElse(null);
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
            IEnergyStorage item = stack.getCapability(ForgeCapabilities.ENERGY).orElse(null);
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
        var entity = server.hasChunkAt(pos) ? server.getBlockEntity(pos) : null;
        return entity == null ? null : entity.getCapability(ForgeCapabilities.ENERGY, side).orElse(null);
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
            int filled = tank.insert(template, resource.getAmount(), getFluidCapacity(), FluidStack::isFluidEqual, action.simulate());
            if (filled > 0 && action.execute()) sync();
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || tank.amount() == 0 ||
                !tank.resource().isFluidEqual(resource)) return FluidStack.EMPTY;
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
