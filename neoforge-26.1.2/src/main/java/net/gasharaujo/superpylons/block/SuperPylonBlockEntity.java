package net.gasharaujo.superpylons.block;

import net.gasharaujo.superpylons.SuperPylonsMod;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class SuperPylonBlockEntity extends PylonBlockEntityBase {
    private final PylonTank<FluidResource> tank = new PylonTank<>();
    private final TankHandler fluidHandler = new TankHandler();

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
        output.putLong("stored_count", getStoredCount());
        output.putLong("fluid_amount", tank.amount());
        if (getStoredCount() > 0) output.store("stored_item", ItemStack.CODEC, getStoredItem());
        if (tank.amount() > 0) output.store("fluid_template", FluidResource.CODEC, tank.resource());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        loadUpgrades(input.getStringOr("upgrades", ""));
        contents.restore(input.read("stored_item", ItemStack.CODEC).orElse(ItemStack.EMPTY), input.getLongOr("stored_count", 0), getItemCapacity());
        FluidResource resource = input.read("fluid_template", FluidResource.CODEC).orElse(FluidResource.EMPTY);
        tank.restore(new PylonTank.State<>(resource.isEmpty() || !canFillFluid(resource.getFluid()) ? null : resource,
            Math.min(getFluidCapacity(), input.getLongOr("fluid_amount", 0))));
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
    protected void fluidModeChanged() {
        if (level != null) level.invalidateCapabilities(worldPosition);
    }

    @Override
    protected ServerPlayer getClickerPlayer(ServerLevel server, GameProfile profile) {
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
