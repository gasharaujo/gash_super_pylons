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
import net.minecraft.resources.Identifier;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
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
    public static final TicketController CHUNK_TICKETS = new TicketController(
        Identifier.fromNamespaceAndPath(SuperPylonsMod.MOD_ID, "pylon"));
    private final ItemPort itemPort = new ItemPort();
    private final PylonTank<FluidResource> tank = new PylonTank<>(this::isCreativeStorage, this::getFluidCapacity);
    private final TankHandler fluidHandler = new TankHandler();
    private final XpFluidPort xpFluidPort = new XpFluidPort();
    private final ResourceHandler<FluidResource> combinedFluids = new CombinedFluids();
    private final EnergyJournal energyJournal = new EnergyJournal();
    private final EnergyHandler energyInput = new EnergyPort(Direction.UP);
    private final EnergyHandler energyInternal = new EnergyPort(null);

    public ResourceHandler<FluidResource> getFluidHandler() {
        if (isTank() && isXpTank()) return combinedFluids;
        return isTank() || isVoid() ? fluidHandler : isXpTank() ? xpFluidPort : null;
    }

    public ResourceHandler<ItemResource> getItemHandler() { return isRemoved() ? null : itemPort; }

    public SuperPylonBlockEntity(BlockPos pos, BlockState state) {
        super(SuperPylonsMod.SUPER_PYLON_ENTITY.get(), pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("upgrades", saveUpgrades());
        if (!getBreakTool().isEmpty()) output.store("break_tool", ItemStack.CODEC, getBreakTool());
        output.putString("paint", savePaint());
        output.putString("wire_id", saveWireId());
        output.putString("wires", saveWires());
        output.putString("wire_inputs", saveWireInputs());
        output.putString("auxiliaries", saveAuxiliaries());
        output.putString("workshop_owner", saveWorkshopOwner());
        output.putString("workshop_status", getWorkshopStatus());
        output.putLong("workshop_cost", getWorkshopCost());
        if (!getShelfItems().isEmpty()) output.store("enchanting_shelves", ItemStack.CODEC.listOf(), getShelfItems());
        output.putString("work_direction", workDirection().getSerializedName());
        if (forcedRadius() > 0) output.putInt("forced_radius", forcedRadius());
        if (hasFilter()) {
            ValueOutput filterData = output.child("filter");
            filterData.putBoolean("allow_list", allowList);
            filterData.putString("tags", saveFilterTags());
            ContainerHelper.saveAllItems(filterData, filterItems(), false);
        }
        if (smeltFuel > 0) output.putLong("smelt_fuel", smeltFuel);
        if (getExperience() > 0) output.putLong("experience", getExperience());
        if (isXpTank()) {
            var xp = xpFluidSnapshot();
            output.putInt("xp_fluid_remainder", xp.remainder());
            output.putString("xp_fluid_id", xp.fluidId());
        }
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
        loadBreakTool(input.read("break_tool", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        loadAuxiliaries(input.getStringOr("auxiliaries", ""));
        loadWorkshopOwner(input.getStringOr("workshop_owner", ""));
        loadWorkshopStatus(input.getStringOr("workshop_status", "connections"), input.getLongOr("workshop_cost", 0));
        loadShelfItems(input.read("enchanting_shelves", ItemStack.CODEC.listOf()).orElse(List.of()));
        loadWorkDirection(input.getStringOr("work_direction", ""));
        loadForcedRadius(input.getIntOr("forced_radius", 0));
        loadDecorations(input.getStringOr("paint", ""), input.getStringOr("wire_id", ""), input.getStringOr("wires", ""));
        loadWireInputs(input.getStringOr("wire_inputs", ""));
        ValueInput filterData = input.childOrEmpty("filter");
        NonNullList<ItemStack> filterEntries = NonNullList.withSize(FILTER_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(filterData, filterEntries);
        loadFilter(filterEntries, filterData.getBooleanOr("allow_list", false));
        loadFilterTags(filterData.getStringOr("tags", ""));
        smeltFuel = Math.max(0, input.getLongOr("smelt_fuel", 0));
        loadSpecialtyAmounts(input.getLongOr("experience", 0), input.getIntOr("energy", 0));
        loadXpFluid(input.getIntOr("xp_fluid_remainder", 0), input.getStringOr("xp_fluid_id", ""));
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
    protected boolean interactWithTank(Player player, InteractionHand hand) {
        return FluidUtil.interactWithFluidHandler(player, hand, worldPosition, getFluidHandler(), null);
    }

    @Override
    protected void forceChunk(ServerLevel server, int x, int z, boolean add) {
        CHUNK_TICKETS.forceChunk(server, worldPosition, x, z, add, false);
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
        if (!isEnergy() && !isVoid()) return null;
        return side == null || side == workDirection() ? energyInternal : energyInput;
    }

    @Override
    protected long orbExperience(ExperienceOrb orb) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, orb.registryAccess());
        orb.saveWithoutId(output);
        return (long) orb.getValue() * Math.max(1, output.buildResult().getIntOr("Count", 1));
    }

    @Override
    protected BiFunction<ItemStack, Boolean, ItemStack> inventoryOutput(ServerLevel server, BlockPos target) {
        ResourceHandler<ItemResource> handler = server.getCapability(Capabilities.Item.BLOCK, target, workDirection().getOpposite());
        return handler == null ? null : (stack, simulate) -> ItemUtil.insertItemReturnRemaining(handler, stack, simulate, null);
    }

    @Override
    protected ItemStack smeltingRecipe(ServerLevel server, ItemStack input) {
        var recipeInput = new SingleRecipeInput(input);
        return server.getServer().getRecipeManager().getRecipeFor(RecipeType.SMELTING, recipeInput, server)
            .map(found -> found.value().assemble(recipeInput))
            .orElse(ItemStack.EMPTY);
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
        if (other.isVoid()) return tank.discard(limit);
        return tank.moveTo(other.tank, limit, other.getFluidCapacity(), FluidResource::equals);
    }

    @Override
    protected PylonExperienceStorage experienceAt(ServerLevel server, BlockPos pos) {
        return server.getCapability(SuperPylonsMod.EXPERIENCE, pos, workDirection().getOpposite());
    }

    @Override
    protected long transferAdjacentExternal(ServerLevel server, BlockPos neighbor, PylonWire.Type type,
                                            boolean importing, long limit) {
        if (!importing && hasBulkTransfer()) {
            long accepted = insertIntoCondenser(server.getBlockEntity(neighbor), type, limit);
            if (accepted >= 0) return accepted;
        }
        return switch (type) {
            case ITEM -> {
                var other = server.getCapability(Capabilities.Item.BLOCK, neighbor, workDirection().getOpposite());
                yield moveResources(importing ? other : itemPort, importing ? itemPort : other, limit);
            }
            case FLUID -> {
                var other = server.getCapability(Capabilities.Fluid.BLOCK, neighbor, workDirection().getOpposite());
                yield moveResources(importing ? other : fluidHandler, importing ? fluidHandler : other, limit);
            }
            case ENERGY -> {
                var other = energyAt(server, neighbor, workDirection().getOpposite());
                yield moveEnergy(importing ? other : energyInternal, importing ? energyInternal : other, limit);
            }
            case XP -> moveLiquidXp(server, neighbor, importing,
                PylonXpFluids.millibuckets(limit));
        };
    }

    private static <T extends net.neoforged.neoforge.transfer.resource.Resource> long moveResources(
            ResourceHandler<T> source, ResourceHandler<T> target, long limit) {
        if (source == null || target == null) return 0;
        long moved = 0;
        while (moved < limit) {
            long before = resourceAmount(source);
            long targetBefore = resourceAmount(target);
            int step = ResourceHandlerUtil.move(source, target, resource -> true, (int) Math.min(Integer.MAX_VALUE, limit - moved), null);
            if (step <= 0) break;
            moved += step;
            boolean targetProgress = resourceAmount(target) > targetBefore && finiteResourceCapacity(target);
            if (limit == Long.MAX_VALUE && resourceAmount(source) >= before && !targetProgress) break;
        }
        return moved;
    }

    private static <T extends net.neoforged.neoforge.transfer.resource.Resource> boolean finiteResourceCapacity(ResourceHandler<T> handler) {
        for (int slot = 0; slot < handler.size(); slot++) {
            if (handler.getCapacityAsLong(slot, handler.getResource(slot)) == Long.MAX_VALUE) return false;
        }
        return true;
    }

    private static long resourceAmount(ResourceHandler<?> handler) {
        long total = 0;
        for (int slot = 0; slot < handler.size(); slot++) {
            long amount = handler.getAmountAsLong(slot);
            if (amount > Long.MAX_VALUE - total) return Long.MAX_VALUE;
            total += amount;
        }
        return total;
    }

    private static long moveEnergy(EnergyHandler source, EnergyHandler target, long limit) {
        if (source == null || target == null) return 0;
        long moved = 0;
        while (moved < limit) {
            long before = source.getAmountAsLong();
            long targetBefore = target.getAmountAsLong();
            int step = EnergyHandlerUtil.move(source, target, (int) Math.min(Integer.MAX_VALUE, limit - moved), null);
            if (step <= 0) break;
            moved += step;
            if (limit == Long.MAX_VALUE && source.getAmountAsLong() >= before && target.getAmountAsLong() <= targetBefore) break;
        }
        return moved;
    }

    private long insertIntoCondenser(BlockEntity target, PylonWire.Type type, long limit) {
        Object template;
        long available;
        Fluid fluid = preferredXpFluid();
        switch (type) {
            case ITEM -> { template = getStoredItem(); available = getAvailableItemCount(); }
            case FLUID -> { template = tank.resource(); available = tank.available(); }
            case XP -> {
                if (fluid == net.minecraft.world.level.material.Fluids.EMPTY) return -1;
                template = FluidResource.of(fluid);
                available = availableXpFluid();
                limit = PylonXpFluids.millibuckets(limit);
            }
            default -> { return -1; }
        }
        var port = PylonCondenserCompat.port(target, type != PylonWire.Type.ITEM, template);
        if (port == null) return -1;
        long moved = port.insert(Math.min(limit, available));
        if (moved > 0) {
            switch (type) {
                case ITEM -> contents.remove(moved);
                case FLUID -> tank.discard(moved);
                case XP -> moveXpFluid(false, fluid, moved);
                default -> { }
            }
            sync();
        }
        return moved;
    }

    private long moveLiquidXp(ServerLevel server, BlockPos neighbor, boolean importing, long limit) {
        var other = server.getCapability(Capabilities.Fluid.BLOCK, neighbor, workDirection().getOpposite());
        if (other == null) return 0;
        if (importing) return moveResources(other, xpFluidPort, limit);
        Fluid preferred = preferredXpFluid();
        long moved = exportLiquidXp(other, preferred, limit);
        if (moved > 0) return moved;
        for (Fluid fluid : PylonXpFluids.available()) {
            if (fluid == preferred) continue;
            moved = exportLiquidXp(other, fluid, limit);
            if (moved > 0) return moved;
        }
        return 0;
    }

    private long exportLiquidXp(ResourceHandler<FluidResource> target, Fluid fluid, long limit) {
        long moved = 0;
        while (moved < limit) {
            long before = xpFluidAmount();
            long targetBefore = resourceAmount(target);
            int step = exportLiquidXpBatch(target, fluid, (int) Math.min(Integer.MAX_VALUE, limit - moved));
            if (step <= 0) break;
            moved += step;
            boolean targetProgress = resourceAmount(target) > targetBefore && finiteResourceCapacity(target);
            if (limit == Long.MAX_VALUE && xpFluidAmount() >= before && !targetProgress) break;
        }
        return moved;
    }

    private int exportLiquidXpBatch(ResourceHandler<FluidResource> target, Fluid fluid, int limit) {
        int available = xpFluidLimit(false, fluid, limit);
        if (available == 0) return 0;
        FluidResource resource = FluidResource.of(fluid);
        try (Transaction tx = Transaction.openRoot()) {
            int accepted = target.insert(resource, available, tx);
            if (accepted == 0 || xpFluidPort.extract(0, resource, accepted, tx) != accepted) return 0;
            tx.commit();
            return accepted;
        }
    }

    @Override
    protected void transferEnergy(ServerLevel server) {
        int inputLimit = energyLimit(true, Integer.MAX_VALUE);
        if (inputLimit > 0 && allowsAutomaticEnergyInput(server, worldPosition.relative(workDirection().getOpposite()))) {
            EnergyHandlerUtil.move(energyAt(server, worldPosition.relative(workDirection().getOpposite()), workDirection()), energyInput, inputLimit, null);
        }
        int outputLimit = energyLimit(false, Integer.MAX_VALUE);
        if (outputLimit > 0 && getStoredCount() == 1) {
            ItemStack stack = getStoredItem();
            EnergyHandler item = ItemAccess.forStack(stack).getCapability(Capabilities.Energy.ITEM);
            if (EnergyHandlerUtil.move(energyInternal, item, outputLimit, null) > 0) {
                contents.restore(stack, 1, getItemCapacity());
            }
        }
        outputLimit = energyLimit(false, Integer.MAX_VALUE);
        if (outputLimit > 0 && allowsAutomaticEnergyOutput()) {
            EnergyHandlerUtil.move(energyInternal, energyAt(server, workTarget(), workDirection().getOpposite()), outputLimit, null);
        }
    }

    private static EnergyHandler energyAt(ServerLevel server, BlockPos pos, Direction side) {
        return server.hasChunkAt(pos) ? server.getCapability(Capabilities.Energy.BLOCK, pos, side) : null;
    }

    private final class ItemPort extends SnapshotJournal<PylonContents.State> implements ResourceHandler<ItemResource> {
        @Override public int size() { return 1; }
        @Override public ItemResource getResource(int index) {
            return index == 0 && canExtractItems() ? ItemResource.of(getStoredItem()) : ItemResource.EMPTY;
        }
        @Override public long getAmountAsLong(int index) { return index == 0 && canExtractItems() ? getAvailableItemCount() : 0; }
        @Override public long getCapacityAsLong(int index, ItemResource resource) {
            if (resource.isEmpty()) return index == 0 && (hasItemStorage() || isVoid()) ? getItemCapacity() : 0;
            return isValid(index, resource) ? getItemCapacity() : 0;
        }
        @Override public boolean isValid(int index, ItemResource resource) {
            return index == 0 && !resource.isEmpty() && canStoreItem(resource.toStack(1));
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonNegative(amount);
            if (!(level instanceof ServerLevel) || index != 0 || resource.isEmpty()) return 0;
            ItemStack stack = resource.toStack(1);
            int accepted = insertItems(stack, amount, true);
            if (isVoid()) return accepted;
            if (accepted == 0) return 0;
            updateSnapshots(transaction);
            return contents.add(stack, accepted, getItemCapacity());
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonNegative(amount);
            if (!(level instanceof ServerLevel) || index != 0 || !canExtractItems() ||
                resource.isEmpty() || !resource.equals(getResource(index))) return 0;
            int extracted = (int) Math.min(amount, getAvailableItemCount());
            if (extracted == 0) return 0;
            updateSnapshots(transaction);
            contents.remove(extracted);
            return extracted;
        }

        @Override protected PylonContents.State createSnapshot() { return contents.snapshot(); }
        @Override protected void revertToSnapshot(PylonContents.State snapshot) { contents.restore(snapshot); }
        @Override protected void onRootCommit(PylonContents.State original) { sync(); }
    }

    private final class EnergyPort implements EnergyHandler {
        private final Direction side;

        private EnergyPort(Direction side) { this.side = side; }

        @Override public long getAmountAsLong() { return isEnergy() ? getAvailableEnergy() : 0; }
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
            if (moved > 0 && !isVoid()) {
                updateSnapshots(transaction);
                energy.transfer(input, moved, level.getGameTime());
            }
            return moved;
        }

        @Override protected PylonEnergy.State createSnapshot() { return energy.snapshot(); }
        @Override protected void revertToSnapshot(PylonEnergy.State snapshot) { energy.restore(snapshot); }
        @Override protected void onRootCommit(PylonEnergy.State original) { energyChanged(); }
    }

    private final class XpFluidPort extends SnapshotJournal<XpFluidState> implements ResourceHandler<FluidResource> {
        @Override public int size() { return 1; }
        @Override public FluidResource getResource(int index) {
            return getAmountAsLong(index) == 0 ? FluidResource.EMPTY : FluidResource.of(preferredXpFluid());
        }
        @Override public long getAmountAsLong(int index) {
            return index == 0 && isXpTank() && !PylonXpFluids.available().isEmpty() ? availableXpFluid() : 0;
        }
        @Override public long getCapacityAsLong(int index, FluidResource resource) {
            return index == 0 && isXpTank() && (resource.isEmpty() || isValid(index, resource)) ? xpFluidCapacity() : 0;
        }
        @Override public boolean isValid(int index, FluidResource resource) {
            return index == 0 && isXpTank() && !resource.isEmpty() && PylonXpFluids.supports(resource.getFluid());
        }
        @Override public int insert(int index, FluidResource resource, int amount, TransactionContext tx) {
            return isValid(index, resource) ? exchange(true, resource.getFluid(), amount, tx) : 0;
        }
        @Override public int extract(int index, FluidResource resource, int amount, TransactionContext tx) {
            return isValid(index, resource) ? exchange(false, resource.getFluid(), amount, tx) : 0;
        }
        private int exchange(boolean input, Fluid fluid, int amount, TransactionContext tx) {
            TransferPreconditions.checkNonNegative(amount);
            int moved = xpFluidLimit(input, fluid, amount);
            if (moved > 0) {
                updateSnapshots(tx);
                moveXpFluid(input, fluid, moved);
            }
            return moved;
        }
        @Override protected XpFluidState createSnapshot() { return xpFluidSnapshot(); }
        @Override protected void revertToSnapshot(XpFluidState snapshot) { restoreXpFluid(snapshot); }
        @Override protected void onRootCommit(XpFluidState original) { sync(); }
    }

    private final class CombinedFluids implements ResourceHandler<FluidResource> {
        private ResourceHandler<FluidResource> port(int index) { return index == 0 ? fluidHandler : xpFluidPort; }
        private boolean validIndex(int index) { return index == 0 || index == 1; }
        @Override public int size() { return 2; }
        @Override public FluidResource getResource(int index) {
            return validIndex(index) ? port(index).getResource(0) : FluidResource.EMPTY;
        }
        @Override public long getAmountAsLong(int index) {
            return validIndex(index) ? port(index).getAmountAsLong(0) : 0;
        }
        @Override public long getCapacityAsLong(int index, FluidResource resource) {
            return validIndex(index) ? port(index).getCapacityAsLong(0, resource) : 0;
        }
        @Override public boolean isValid(int index, FluidResource resource) {
            return validIndex(index) && port(index).isValid(0, resource);
        }
        @Override public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return validIndex(index) ? port(index).insert(0, resource, amount, transaction) : 0;
        }
        @Override public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return validIndex(index) ? port(index).extract(0, resource, amount, transaction) : 0;
        }
    }

    private final class TankHandler extends SnapshotJournal<PylonTank.State<FluidResource>>
        implements ResourceHandler<FluidResource> {
        @Override public int size() { return 1; }

        @Override
        public FluidResource getResource(int index) {
            return index == 0 && isTank() && tank.amount() > 0 ? tank.resource() : FluidResource.EMPTY;
        }

        @Override public long getAmountAsLong(int index) { return index == 0 && isTank() ? tank.available() : 0; }
        @Override public long getCapacityAsLong(int index, FluidResource resource) { return index == 0 ? getFluidCapacity() : 0; }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            return index == 0 && !resource.isEmpty() && canFillFluid(resource.getFluid());
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            if (!isValid(index, resource) || isRemoved()) return 0;
            if (isVoid()) return amount;
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
