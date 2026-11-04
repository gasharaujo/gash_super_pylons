package net.gasharaujo.superpylons.block;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.function.BiFunction;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public final class SuperPylonBlockEntity extends PylonBlockEntityBase {
    private final PylonTank<FluidResource> tank = new PylonTank<>();
    private final TankHandler fluidHandler = new TankHandler();
    private final EnergyJournal energyJournal = new EnergyJournal();
    private final EnergyHandler energyInput = new EnergyPort(Direction.UP);
    private final EnergyHandler energyOutput = new EnergyPort(Direction.DOWN);
    private final EnergyHandler energyInternal = new EnergyPort(null);

    public ResourceHandler<FluidResource> getFluidHandler() {
        return isTank() ? fluidHandler : null;
    }

    public SuperPylonBlockEntity(BlockPos pos, BlockState state) {
        super(SuperPylonsMod.SUPER_PYLON_ENTITY.get(), pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("upgrades", saveUpgrades());
        output.putString("paint", savePaint());
        output.putString("wire_id", saveWireId());
        output.putString("wires", saveWires());
        if (hasFilter()) {
            ValueOutput filterData = output.child("filter");
            filterData.putBoolean("allow_list", allowList);
            ContainerHelper.saveAllItems(filterData, filterItems(), false);
        }
        if (smeltFuel > 0) output.putLong("smelt_fuel", smeltFuel);
        if (getExperience() > 0) output.putLong("experience", getExperience());
        if (getEnergy() > 0) output.putInt("energy", getEnergy());
        if (!outputBuffer.isEmpty()) output.store("harvest", ItemStack.CODEC.listOf(), outputBuffer);
        output.putLong("stored_count", getStoredCount());
        output.putLong("fluid_amount", tank.amount());
        if (getStoredCount() > 0) output.store("stored_item", ItemStack.CODEC, getStoredItem());
        if (tank.amount() > 0) output.store("fluid_template", FluidResource.CODEC, tank.resource());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        loadUpgrades(input.getStringOr("upgrades", ""));
        loadDecorations(input.getStringOr("paint", ""), input.getStringOr("wire_id", ""), input.getStringOr("wires", ""));
        ValueInput filterData = input.childOrEmpty("filter");
        NonNullList<ItemStack> filterEntries = NonNullList.withSize(FILTER_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(filterData, filterEntries);
        loadFilter(filterEntries, filterData.getBooleanOr("allow_list", false));
        smeltFuel = getSpecialty() == PylonUpgrade.SMELT ? Math.max(0, input.getLongOr("smelt_fuel", 0)) : 0;
        loadSpecialtyAmounts(input.getLongOr("experience", 0), input.getIntOr("energy", 0));
        outputBuffer.clear();
        outputBuffer.addAll(input.read("harvest", ItemStack.CODEC.listOf()).orElse(List.of()));
        contents.restore(input.read("stored_item", ItemStack.CODEC).orElse(ItemStack.EMPTY), input.getLongOr("stored_count", 0), getItemCapacity());
        FluidResource resource = input.read("fluid_template", FluidResource.CODEC).orElse(FluidResource.EMPTY);
        tank.restore(new PylonTank.State<>(resource.isEmpty() || !canFillFluid(resource.getFluid()) ? null : resource,
            input.getLongOr("fluid_amount", 0)));
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
        int added = tank.insert(FluidResource.of(fluid), amount, getFluidCapacity(), FluidResource::equals, false);
        if (added > 0) sync();
    }

    @Override
    protected void interactWithTank(Player player, InteractionHand hand) {
        FluidUtil.interactWithFluidHandler(player, hand, worldPosition, fluidHandler, null);
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
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        saveCustomOnly(output);
        BlockItem.setBlockEntityData(stack, getType(), output);
        return stack;
    }

    @Override
    protected void notifyPlayer(Player player, Component message) {
        player.sendOverlayMessage(message);
    }

    public EnergyHandler getEnergyHandler(Direction side) {
        if (!isEnergy()) return null;
        if (side == null) return energyInternal;
        if (side == Direction.UP) return energyInput;
        return side == Direction.DOWN ? energyOutput : null;
    }

    @Override
    protected long orbExperience(ExperienceOrb orb) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, orb.registryAccess());
        orb.saveWithoutId(output);
        return (long) orb.getValue() * Math.max(1, output.buildResult().getIntOr("Count", 1));
    }

    @Override
    protected BiFunction<ItemStack, Boolean, ItemStack> inventoryOutput(ServerLevel server, BlockPos target) {
        ResourceHandler<ItemResource> handler = server.getCapability(Capabilities.Item.BLOCK, target, Direction.UP);
        return handler == null ? null : (stack, simulate) -> ItemUtil.insertItemReturnRemaining(handler, stack, simulate, null);
    }

    @Override
    protected SmeltingResult smeltingRecipe(ServerLevel server, ItemStack input) {
        var recipeInput = new SingleRecipeInput(input);
        return server.getServer().getRecipeManager().getRecipeFor(RecipeType.SMELTING, recipeInput, server)
            .map(found -> new SmeltingResult(found.value().assemble(recipeInput), found.value().cookingTime()))
            .orElse(null);
    }

    @Override
    protected int takeSmeltingFuel(ServerLevel server, BlockPos pos, Direction side) {
        ResourceHandler<ItemResource> handler = server.getCapability(Capabilities.Item.BLOCK, pos, side);
        if (handler == null) return 0;
        for (int slot = 0; slot < handler.size(); slot++) {
            ItemResource resource = handler.getResource(slot);
            if (resource.isEmpty()) continue;
            ItemStack stack = resource.toStack(1);
            int burnTime = stack.getBurnTime(RecipeType.SMELTING, server.fuelValues());
            if (burnTime <= 0) continue;
            ItemStack leftover;
            try (Transaction tx = Transaction.openRoot()) {
                if (handler.extract(slot, resource, 1, tx) != 1) continue;
                var remainder = stack.getCraftingRemainder();
                leftover = remainder == null ? ItemStack.EMPTY :
                    ItemUtil.insertItemReturnRemaining(handler, remainder.create(), false, tx);
                tx.commit();
            }
            if (!leftover.isEmpty()) Block.popResource(server, pos, leftover);
            return burnTime;
        }
        return 0;
    }

    @Override
    protected long transferFluidTo(PylonBlockEntityBase target, long limit) {
        var resource = tank.resource();
        if (resource == null || !(target instanceof SuperPylonBlockEntity other) || !other.canFillFluid(resource.getFluid())) return 0;
        return tank.moveTo(other.tank, limit, other.getFluidCapacity(), FluidResource::equals);
    }

    @Override
    protected void transferEnergy(ServerLevel server) {
        int inputLimit = energyLimit(true, Integer.MAX_VALUE);
        if (inputLimit > 0) {
            EnergyHandlerUtil.move(energyAt(server, worldPosition.above(), Direction.DOWN), energyInput, inputLimit, null);
        }
        int outputLimit = energyLimit(false, Integer.MAX_VALUE);
        if (outputLimit > 0 && getStoredCount() == 1) {
            ItemStack stack = getStoredItem();
            EnergyHandler item = ItemAccess.forStack(stack).getCapability(Capabilities.Energy.ITEM);
            if (EnergyHandlerUtil.move(energyOutput, item, outputLimit, null) > 0) {
                contents.restore(stack, 1, getItemCapacity());
            }
        }
        outputLimit = energyLimit(false, Integer.MAX_VALUE);
        if (outputLimit > 0) {
            EnergyHandlerUtil.move(energyOutput, energyAt(server, worldPosition.below(), Direction.UP), outputLimit, null);
        }
    }

    private static EnergyHandler energyAt(ServerLevel server, BlockPos pos, Direction side) {
        return server.hasChunkAt(pos) ? server.getCapability(Capabilities.Energy.BLOCK, pos, side) : null;
    }

    private final class EnergyPort implements EnergyHandler {
        private final Direction side;

        private EnergyPort(Direction side) { this.side = side; }

        @Override public long getAmountAsLong() { return isEnergy() ? getEnergy() : 0; }
        @Override public long getCapacityAsLong() { return getEnergyCapacity(); }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonNegative(amount);
            return side == Direction.DOWN ? 0 : energyJournal.exchange(true, amount, transaction);
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonNegative(amount);
            return side == Direction.UP ? 0 : energyJournal.exchange(false, amount, transaction);
        }
    }

    private final class EnergyJournal extends SnapshotJournal<PylonEnergy.State> {
        int exchange(boolean input, int requested, TransactionContext transaction) {
            int moved = energyLimit(input, requested);
            if (moved > 0) {
                updateSnapshots(transaction);
                energy.transfer(input, moved, level.getGameTime());
            }
            return moved;
        }

        @Override protected PylonEnergy.State createSnapshot() { return energy.snapshot(); }
        @Override protected void revertToSnapshot(PylonEnergy.State snapshot) { energy.restore(snapshot); }
        @Override protected void onRootCommit(PylonEnergy.State original) { energyChanged(); }
    }

    private final class TankHandler extends SnapshotJournal<PylonTank.State<FluidResource>>
        implements ResourceHandler<FluidResource> {
        @Override public int size() { return 1; }

        @Override
        public FluidResource getResource(int index) {
            return index == 0 && isTank() && tank.amount() > 0 ? tank.resource() : FluidResource.EMPTY;
        }

        @Override public long getAmountAsLong(int index) { return index == 0 && isTank() ? tank.amount() : 0; }
        @Override public long getCapacityAsLong(int index, FluidResource resource) { return index == 0 ? getFluidCapacity() : 0; }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            return index == 0 && !resource.isEmpty() && canFillFluid(resource.getFluid());
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            if (!isValid(index, resource) || isRemoved()) return 0;
            int accepted = tank.insert(resource, amount, getFluidCapacity(), FluidResource::equals, true);
            if (accepted > 0) {
                updateSnapshots(transaction);
                tank.insert(resource, accepted, getFluidCapacity(), FluidResource::equals, false);
            }
            return accepted;
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            if (isRemoved() || resource.isEmpty() || !resource.equals(getResource(index))) return 0;
            int removed = tank.extract(amount, true);
            if (removed > 0) {
                updateSnapshots(transaction);
                tank.extract(removed, false);
            }
            return removed;
        }

        @Override protected PylonTank.State<FluidResource> createSnapshot() { return tank.snapshot(); }
        @Override protected void revertToSnapshot(PylonTank.State<FluidResource> snapshot) { tank.restore(snapshot); }
        @Override protected void onRootCommit(PylonTank.State<FluidResource> original) { sync(); }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        dropContents();
        super.preRemoveSideEffects(pos, state);
    }

}
