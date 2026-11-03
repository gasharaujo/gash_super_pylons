package net.gasharaujo.superpylons.block;

import net.gasharaujo.superpylons.SuperPylonsMod;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class SuperPylonBlockEntity extends PylonBlockEntityBase {
    private final PylonTank<FluidStack> tank = new PylonTank<>();
    private final TankHandler fluidHandler = new TankHandler();

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
        tag.putLong("stored_count", getStoredCount());
        tag.putLong("fluid_amount", tank.amount());
        if (getStoredCount() > 0) tag.put("stored_item", getStoredItem().save(new CompoundTag()));
        if (tank.amount() > 0) tag.put("fluid_template", tank.resource().writeToNBT(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        loadUpgrades(tag.getString("upgrades"));
        contents.restore(ItemStack.of(tag.getCompound("stored_item")), tag.getLong("stored_count"), getItemCapacity());
        FluidStack resource = FluidStack.loadFluidStackFromNBT(tag.getCompound("fluid_template"));
        tank.restore(new PylonTank.State<>(resource.isEmpty() || !canFillFluid(resource.getFluid()) ? null : resource,
            Math.min(getFluidCapacity(), tag.getLong("fluid_amount"))));
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
    protected void fluidModeChanged() {
        tankCapability.invalidate();
        tankCapability = LazyOptional.of(() -> fluidHandler);
    }

    @Override
    protected ServerPlayer getClickerPlayer(ServerLevel server, GameProfile profile) {
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

    private LazyOptional<IFluidHandler> tankCapability = LazyOptional.of(() -> fluidHandler);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER && isTank() && !isRemoved()) return tankCapability.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        tankCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        tankCapability = LazyOptional.of(() -> fluidHandler);
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
