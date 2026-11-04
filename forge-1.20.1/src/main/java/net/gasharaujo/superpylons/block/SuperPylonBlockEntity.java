package net.gasharaujo.superpylons.block;

import com.mojang.authlib.GameProfile;
import java.util.function.BiFunction;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.ForgeHooks;
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
    private final ItemPort itemPort = new ItemPort();
    private final PylonTank<FluidStack> tank = new PylonTank<>();
    private final TankHandler fluidHandler = new TankHandler();
    private final XpFluidPort xpFluidPort = new XpFluidPort();
    private final EnergyPort energyInput = new EnergyPort(Direction.UP);
    private final EnergyPort energyOutput = new EnergyPort(Direction.DOWN);
    private final EnergyPort energyInternal = new EnergyPort(null);
    private LazyOptional<IEnergyStorage> inputCapability = LazyOptional.of(() -> energyInput);
    private LazyOptional<IEnergyStorage> outputCapability = LazyOptional.of(() -> energyOutput);
    private LazyOptional<IEnergyStorage> internalCapability = LazyOptional.of(() -> energyInternal);
    private LazyOptional<IFluidHandler> tankCapability = LazyOptional.of(() -> fluidHandler);
    private LazyOptional<IFluidHandler> xpFluidCapability = LazyOptional.of(() -> xpFluidPort);
    private LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> itemPort);
    private LazyOptional<PylonExperienceStorage> experienceCapability = LazyOptional.of(() -> this);

    public IFluidHandler getFluidHandler() {
        return isTank() ? fluidHandler : isXpTank() ? xpFluidPort : null;
    }

    public SuperPylonBlockEntity(BlockPos pos, BlockState state) {
        super(SuperPylonsMod.SUPER_PYLON_ENTITY.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("upgrades", saveUpgrades());
        if (!getBreakTool().isEmpty()) tag.put("break_tool", getBreakTool().save(new CompoundTag()));
        tag.putString("paint", savePaint());
        tag.putString("wire_id", saveWireId());
        tag.putString("wires", saveWires());
        tag.putString("auxiliaries", saveAuxiliaries());
        if (hasFilter()) {
            CompoundTag filterData = new CompoundTag();
            filterData.putBoolean("allow_list", allowList);
            ContainerHelper.saveAllItems(filterData, filterItems());
            tag.put("filter", filterData);
        }
        if (smeltFuel > 0) tag.putLong("smelt_fuel", smeltFuel);
        if (getExperience() > 0) tag.putLong("experience", getExperience());
        if (isXpTank()) {
            var xp = xpFluidSnapshot();
            tag.putInt("xp_fluid_remainder", xp.remainder());
            tag.putString("xp_fluid_id", xp.fluidId());
        }
        if (getEnergy() > 0) tag.putInt("energy", getEnergy());
        if (!outputBuffer.isEmpty()) {
            ListTag harvest = new ListTag();
            for (ItemStack stack : outputBuffer) harvest.add(stack.save(new CompoundTag()));
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
        loadBreakTool(ItemStack.of(tag.getCompound("break_tool")));
        loadAuxiliaries(tag.getString("auxiliaries"));
        loadDecorations(tag.getString("paint"), tag.getString("wire_id"), tag.getString("wires"));
        CompoundTag filterData = tag.getCompound("filter");
        NonNullList<ItemStack> filterEntries = NonNullList.withSize(FILTER_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(filterData, filterEntries);
        loadFilter(filterEntries, filterData.getBoolean("allow_list"));
        smeltFuel = Math.max(0, tag.getLong("smelt_fuel"));
        loadSpecialtyAmounts(tag.getLong("experience"), tag.getInt("energy"));
        loadXpFluid(tag.getInt("xp_fluid_remainder"), tag.getString("xp_fluid_id"));
        outputBuffer.clear();
        ListTag harvest = tag.getList("harvest", Tag.TAG_COMPOUND);
        for (int i = 0; i < harvest.size(); i++) {
            ItemStack stack = ItemStack.of(harvest.getCompound(i));
            if (!stack.isEmpty()) outputBuffer.add(stack);
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
        if (cap == SuperPylonsMod.EXPERIENCE && !isRemoved()) return experienceCapability.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER && !isRemoved()) return itemCapability.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER && isTank() && !isRemoved()) return tankCapability.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER && isXpTank() && !isRemoved()) return xpFluidCapability.cast();
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
        experienceCapability.invalidate();
        itemCapability.invalidate();
        tankCapability.invalidate();
        xpFluidCapability.invalidate();
        inputCapability.invalidate();
        outputCapability.invalidate();
        internalCapability.invalidate();
    }

    private void createPylonCapabilities() {
        experienceCapability = LazyOptional.of(() -> this);
        itemCapability = LazyOptional.of(() -> itemPort);
        tankCapability = LazyOptional.of(() -> fluidHandler);
        xpFluidCapability = LazyOptional.of(() -> xpFluidPort);
        inputCapability = LazyOptional.of(() -> energyInput);
        outputCapability = LazyOptional.of(() -> energyOutput);
        internalCapability = LazyOptional.of(() -> energyInternal);
    }

    @Override
    protected long orbExperience(ExperienceOrb orb) {
        return (long) orb.getValue() * Math.max(1, orb.saveWithoutId(new CompoundTag()).getInt("Count"));
    }

    @Override
    protected BiFunction<ItemStack, Boolean, ItemStack> inventoryOutput(ServerLevel server, BlockPos target) {
        IItemHandler handler = itemHandlerAt(server, target, Direction.UP);
        return handler == null ? null : (stack, simulate) -> ItemHandlerHelper.insertItemStacked(handler, stack, simulate);
    }


    @Override
    protected ItemStack smeltingRecipe(ServerLevel server, ItemStack input) {
        var recipeInput = new SimpleContainer(input);
        return server.getRecipeManager().getRecipeFor(RecipeType.SMELTING, recipeInput, server)
            .map(found -> found.assemble(recipeInput, server.registryAccess()))
            .orElse(ItemStack.EMPTY);
    }

    @Override
    protected int takeSmeltingFuel(ServerLevel server, BlockPos pos, Direction side) {
        IItemHandler handler = itemHandlerAt(server, pos, side);
        if (handler == null) return 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack fuel = handler.extractItem(slot, 1, true);
            int burnTime = ForgeHooks.getBurnTime(fuel, RecipeType.SMELTING);
            if (burnTime <= 0) continue;
            fuel = handler.extractItem(slot, 1, false);
            if (fuel.isEmpty()) continue;
            ItemStack remainder = fuel.getCraftingRemainingItem();
            if (!remainder.isEmpty()) {
                ItemStack leftover = ItemHandlerHelper.insertItemStacked(handler, remainder, false);
                if (!leftover.isEmpty()) Block.popResource(server, pos, leftover);
            }
            return burnTime;
        }
        return 0;
    }

    private static IItemHandler itemHandlerAt(ServerLevel server, BlockPos pos, Direction side) {
        var entity = server.getBlockEntity(pos);
        return entity == null ? null : entity.getCapability(ForgeCapabilities.ITEM_HANDLER, side).orElse(null);
    }

    @Override
    protected long transferFluidTo(PylonBlockEntityBase target, long limit) {
        var resource = tank.resource();
        if (resource == null || !(target instanceof SuperPylonBlockEntity other) || !other.canFillFluid(resource.getFluid())) return 0;
        return tank.moveTo(other.tank, limit, other.getFluidCapacity(), FluidStack::isFluidEqual);
    }

    @Override
    protected PylonExperienceStorage experienceAt(ServerLevel server, BlockPos pos) {
        var entity = server.getBlockEntity(pos);
        return entity == null ? null : entity.getCapability(SuperPylonsMod.EXPERIENCE, Direction.UP).orElse(null);
    }

    @Override
    protected long transferAdjacentExternal(ServerLevel server, BlockPos neighbor, PylonWire.Type type,
                                            boolean importing, long limit) {
        int maximum = (int) Math.min(Integer.MAX_VALUE, limit);
        return switch (type) {
            case ITEM -> moveItems(server, neighbor, importing, maximum);
            case FLUID -> {
                var entity = server.getBlockEntity(neighbor);
                var other = entity == null ? null : entity.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP).orElse(null);
                yield moveFluid(importing ? other : fluidHandler, importing ? fluidHandler : other, maximum);
            }
            case ENERGY -> {
                var other = energyAt(server, neighbor, Direction.UP);
                yield moveEnergy(importing ? other : energyInternal, importing ? energyInternal : other, maximum);
            }
            case XP -> moveLiquidXp(server, neighbor, importing,
                (int) Math.min(Integer.MAX_VALUE, PylonXpFluids.millibuckets(limit)));
        };
    }

    private int moveLiquidXp(ServerLevel server, BlockPos neighbor, boolean importing, int limit) {
        var entity = server.getBlockEntity(neighbor);
        var other = entity == null ? null : entity.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP).orElse(null);
        if (other == null) return 0;
        if (importing) return moveFluid(other, xpFluidPort, limit);
        Fluid preferred = preferredXpFluid();
        int moved = moveFluid(xpFluidPort, other, new FluidStack(preferred, limit));
        if (moved > 0) return moved;
        for (Fluid fluid : PylonXpFluids.available()) {
            if (fluid == preferred) continue;
            moved = moveFluid(xpFluidPort, other, new FluidStack(fluid, limit));
            if (moved > 0) return moved;
        }
        return 0;
    }

    private int moveItems(ServerLevel server, BlockPos neighbor, boolean importing, int limit) {
        var handler = itemHandlerAt(server, neighbor, Direction.UP);
        if (handler == null) return 0;
        int moved = 0;
        for (int slot = 0; slot < handler.getSlots() && moved < limit; slot++) {
            if (importing) {
                ItemStack offered = handler.extractItem(slot, limit - moved, true);
                int wanted = offered.getCount() - insertAutomation(offered, true).getCount();
                if (wanted == 0) continue;
                ItemStack extracted = handler.extractItem(slot, wanted, false);
                ItemStack remaining = insertAutomation(extracted, false);
                moved += extracted.getCount() - remaining.getCount();
                if (!remaining.isEmpty()) {
                    Block.popResource(server, neighbor, ItemHandlerHelper.insertItemStacked(handler, remaining, false));
                }
            } else {
                ItemStack offered = extractAutomation(limit - moved, true);
                if (offered.isEmpty()) break;
                int wanted = offered.getCount() - handler.insertItem(slot, offered, true).getCount();
                if (wanted == 0) continue;
                ItemStack extracted = extractAutomation(wanted, false);
                ItemStack remaining = handler.insertItem(slot, extracted, false);
                moved += extracted.getCount() - remaining.getCount();
                if (!remaining.isEmpty()) Block.popResource(server, worldPosition, insertAutomation(remaining, false));
            }
        }
        return moved;
    }

    private static int moveFluid(IFluidHandler source, IFluidHandler target, int limit) {
        if (source == null || target == null) return 0;
        for (int slot = 0; slot < source.getTanks(); slot++) {
            FluidStack available = source.getFluidInTank(slot).copy();
            available.setAmount(Math.min(available.getAmount(), limit));
            int moved = moveFluid(source, target, available);
            if (moved > 0) return moved;
        }
        return 0;
    }

    private static int moveFluid(IFluidHandler source, IFluidHandler target, FluidStack request) {
        if (request.isEmpty()) return 0;
        FluidStack offered = source.drain(request, IFluidHandler.FluidAction.SIMULATE);
        int accepted = target.fill(offered, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return 0;
        offered.setAmount(Math.min(accepted, offered.getAmount()));
        FluidStack extracted = source.drain(offered, IFluidHandler.FluidAction.EXECUTE);
        int moved = target.fill(extracted, IFluidHandler.FluidAction.EXECUTE);
        if (moved < extracted.getAmount()) {
            extracted.shrink(moved);
            source.fill(extracted, IFluidHandler.FluidAction.EXECUTE);
        }
        return moved;
    }

    private static int moveEnergy(IEnergyStorage source, IEnergyStorage target, int limit) {
        if (source == null || target == null) return 0;
        int offered = source.extractEnergy(limit, true);
        int accepted = target.receiveEnergy(offered, true);
        if (accepted == 0) return 0;
        int extracted = source.extractEnergy(accepted, false);
        int moved = target.receiveEnergy(extracted, false);
        if (moved < extracted) source.receiveEnergy(extracted - moved, false);
        return moved;
    }

    @Override
    protected void transferEnergy(ServerLevel server) {
        int inputLimit = energyLimit(true, Integer.MAX_VALUE);
        if (inputLimit > 0 && allowsAutomaticEnergyInput(server, worldPosition.above())) {
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
        if (outputLimit > 0 && allowsAutomaticEnergyOutput()) {
            IEnergyStorage target = energyAt(server, worldPosition.below(), Direction.UP);
            if (target != null) exchangeEnergy(false, target.receiveEnergy(outputLimit, false), false);
        }
    }

    private static IEnergyStorage energyAt(ServerLevel server, BlockPos pos, Direction side) {
        var entity = server.hasChunkAt(pos) ? server.getBlockEntity(pos) : null;
        return entity == null ? null : entity.getCapability(ForgeCapabilities.ENERGY, side).orElse(null);
    }

    private final class ItemPort implements IItemHandler {
        @Override public int getSlots() { return 1; }
        @Override public ItemStack getStackInSlot(int slot) {
            return slot == 0 && canExtractItems() ?
                getStoredItem().copyWithCount((int) Math.min(Integer.MAX_VALUE, getStoredCount())) : ItemStack.EMPTY;
        }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == 0 ? insertAutomation(stack, simulate) : stack;
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 0 ? extractAutomation(amount, simulate) : ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) {
            return slot == 0 && hasItemStorage() ? (int) Math.min(Integer.MAX_VALUE, getItemCapacity()) : 0;
        }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return slot == 0 && canStoreItem(stack); }
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

    private final class XpFluidPort implements IFluidHandler {
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int tank) {
            return tank == 0 && isXpTank() && !PylonXpFluids.available().isEmpty() ?
                new FluidStack(preferredXpFluid(), (int) Math.min(Integer.MAX_VALUE, xpFluidAmount())) : FluidStack.EMPTY;
        }
        @Override public int getTankCapacity(int tank) {
            return tank == 0 ? (int) Math.min(Integer.MAX_VALUE, xpFluidCapacity()) : 0;
        }
        @Override public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && isXpTank() && !stack.isEmpty() && PylonXpFluids.supports(stack.getFluid());
        }
        @Override public int fill(FluidStack stack, FluidAction action) {
            return isFluidValid(0, stack) ? exchange(true, stack.getFluid(), stack.getAmount(), action) : 0;
        }
        @Override public FluidStack drain(FluidStack stack, FluidAction action) {
            return isFluidValid(0, stack) ? drain(stack.getFluid(), stack.getAmount(), action) : FluidStack.EMPTY;
        }
        @Override public FluidStack drain(int amount, FluidAction action) {
            return drain(preferredXpFluid(), amount, action);
        }
        private FluidStack drain(Fluid fluid, int requested, FluidAction action) {
            int moved = exchange(false, fluid, requested, action);
            return moved == 0 ? FluidStack.EMPTY : new FluidStack(fluid, moved);
        }
        private int exchange(boolean input, Fluid fluid, int amount, FluidAction action) {
            int moved = xpFluidLimit(input, fluid, amount);
            if (moved > 0 && action.execute()) {
                moveXpFluid(input, fluid, moved);
                sync();
            }
            return moved;
        }
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
