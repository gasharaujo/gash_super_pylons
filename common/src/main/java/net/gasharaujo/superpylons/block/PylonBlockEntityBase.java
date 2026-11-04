package net.gasharaujo.superpylons.block;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public abstract class PylonBlockEntityBase extends BlockEntity {
    private static final GameProfile AUTOMATION_PROFILE =
        new GameProfile(UUID.fromString("507ad2bf-5600-42ed-9fc4-4b7076fc34b5"), "[Super Pylon]");
    private static final int BASE_FLUID_CAPACITY_MB = 32_000;
    private static final int CONFIRM_TICKS = 100;
    private static final int MAX_LOOSE_STACKS = 64;
    private static final int SMELT_ENERGY_COST = 1000;
    private static final int VERTICAL_RANGE = 1;
    static final int FILTER_SIZE = 27;

    protected final PylonContents contents = new PylonContents();
    protected final PylonEnergy energy = new PylonEnergy();
    protected final List<ItemStack> outputBuffer = new ArrayList<>();
    protected final SimpleContainer filter = new SimpleContainer(FILTER_SIZE);
    protected boolean allowList;
    protected long smeltFuel;
    private final EnumMap<PylonUpgrade.Slot, PylonUpgrade> upgrades = new EnumMap<>(PylonUpgrade.Slot.class);
    private long experience;
    private boolean energyUpdatePending;
    private UUID warnedPlayer;
    private long warningExpires;
    private boolean contentsDropped;
    private boolean packedDrop;

    protected PylonBlockEntityBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public long getStoredCount() { return contents.count(); }
    public ItemStack getStoredItem() { return contents.displayItem(); }
    public long getItemCapacity() {
        return isEnergy() ? 1 : PylonUpgrade.itemCapacity(upgrades.get(PylonUpgrade.Slot.STORAGE));
    }

    public PylonUpgrade getSpecialty() { return upgrades.get(PylonUpgrade.Slot.SPECIALTY); }

    public List<PylonUpgrade> getInstalledUpgrades() { return List.copyOf(upgrades.values()); }

    public boolean hasFilter() { return upgrades.containsKey(PylonUpgrade.Slot.FILTER); }

    protected NonNullList<ItemStack> filterItems() {
        NonNullList<ItemStack> entries = NonNullList.withSize(FILTER_SIZE, ItemStack.EMPTY);
        for (int i = 0; i < FILTER_SIZE; i++) entries.set(i, filter.getItem(i));
        return entries;
    }

    protected void loadFilter(NonNullList<ItemStack> entries, boolean allowList) {
        filter.clearContent();
        this.allowList = hasFilter() && allowList;
        if (!hasFilter()) return;
        for (int i = 0; i < FILTER_SIZE; i++) filter.setItem(i, entries.get(i).copyWithCount(1));
    }

    public long getExperience() { return experience; }
    public boolean isEnergy() { return getEnergyCapacity() > 0; }
    public int getEnergy() { return energy.amount(); }

    public int getEnergyCapacity() {
        PylonUpgrade specialty = getSpecialty();
        return specialty != null && specialty.isEnergy() ? specialty.value() : 0;
    }

    protected void loadSpecialtyAmounts(long savedExperience, int savedEnergy) {
        experience = getSpecialty() == PylonUpgrade.XP_TANK ? Math.max(0, savedExperience) : 0;
        energy.restore(new PylonEnergy.State(Math.max(0, Math.min(getEnergyCapacity(), savedEnergy)),
            Long.MIN_VALUE, 0, 0));
    }

    protected int energyLimit(boolean input, int requested) {
        if (!(level instanceof ServerLevel) || isRemoved() || !isEnergy()) return 0;
        return energy.limit(input, requested, getEnergyCapacity(), getSpecialty().energyTransfer(), level.getGameTime());
    }

    protected void energyChanged() {
        setChanged();
        energyUpdatePending = true;
    }

    public boolean isTank() {
        PylonUpgrade specialty = getSpecialty();
        return specialty != null && specialty.isTank();
    }

    public long getFluidCapacity() {
        return isTank() ? PylonUpgrade.storageCapacity(BASE_FLUID_CAPACITY_MB, upgrades.get(PylonUpgrade.Slot.STORAGE)) : 0;
    }

    public boolean canFillFluid(Fluid fluid) {
        PylonUpgrade specialty = getSpecialty();
        return specialty != null && specialty.isTank() &&
            (!specialty.generatesFluid() || specialty.generatedFluid() == fluid);
    }

    public abstract long getFluidAmount();
    public abstract Component getFluidName();
    protected abstract void clearFluid();
    protected abstract void generateFluid(Fluid fluid, int amount);
    protected abstract void interactWithTank(Player player, InteractionHand hand);
    protected abstract void capabilitiesChanged();
    protected abstract ServerPlayer getAutomationPlayer(ServerLevel level, GameProfile profile);
    protected abstract ItemStack createPackedDrop();
    protected abstract void notifyPlayer(Player player, Component message);
    protected abstract long orbExperience(ExperienceOrb orb);
    protected abstract void transferEnergy(ServerLevel server);
    protected abstract BiFunction<ItemStack, Boolean, ItemStack> inventoryOutput(ServerLevel server, BlockPos target);
    protected abstract SmeltingResult smeltingRecipe(ServerLevel server, ItemStack input);
    protected abstract int takeSmeltingFuel(ServerLevel server, BlockPos pos, Direction side);

    protected record SmeltingResult(ItemStack output, int fuelTicks) {}

    private Item generatedItem() {
        PylonUpgrade specialty = getSpecialty();
        return specialty == null ? Items.AIR : specialty.generatedItem();
    }

    public int insertFrom(ItemStack input) {
        if (!(level instanceof ServerLevel) || isRemoved()) return 0;
        PylonUpgrade specialty = getSpecialty();
        if (specialty != null && specialty.locksItems()) return 0;
        if (!allowsItem(input)) return 0;
        Item output = generatedItem();
        if (output != Items.AIR && !PylonItemRules.exactlyEqual(new ItemStack(output), input)) return 0;
        int inserted = contents.insert(input, getItemCapacity());
        if (inserted > 0) sync();
        return inserted;
    }

    private boolean allowsItem(ItemStack input) {
        if (!hasFilter() || input.isEmpty()) return true;
        for (int i = 0; i < FILTER_SIZE; i++) {
            if (PylonItemRules.exactlyEqual(filter.getItem(i), input)) return allowList;
        }
        return !allowList;
    }

    public boolean use(Player player, InteractionHand hand) {
        if (player.isSpectator()) return false;
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            if (hand != InteractionHand.MAIN_HAND) return false;
            if (player.isShiftKeyDown()) return tryRemoveUpgrade(player);
            if (!hasFilter() || !player.getOffhandItem().isEmpty()) return false;
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inventory, viewer) -> new PylonFilterMenu(id, inventory, this),
                    Component.translatable("menu.gash_super_pylons.filter")));
            }
            return true;
        }
        if (held.getItem() instanceof PylonUpgradeItem upgrade) {
            if (level instanceof ServerLevel) install(player, held, upgrade.upgrade());
            return true;
        }
        if (PylonItemRules.isModifier(held)) return false;
        if (level instanceof ServerLevel) {
            if (isTank()) interactWithTank(player, hand);
            else insertFrom(held);
        }
        return true;
    }

    private void install(Player player, ItemStack held, PylonUpgrade next) {
        if (next == PylonUpgrade.PULVERIZE) {
            warn(player, "upgrade_disabled");
            return;
        }
        PylonUpgrade previous = upgrades.get(next.slot());
        if (previous != null && (previous.tier() >= next.tier() ||
            (next.slot() == PylonUpgrade.Slot.SPECIALTY && !(previous.isEnergy() && next.isEnergy())))) {
            warn(player, "upgrade_slot_occupied");
            return;
        }
        if (next.locksItems() && getStoredCount() > 0) {
            warn(player, "upgrade_needs_empty");
            return;
        }
        if (next.isEnergy() && getStoredCount() > 1) {
            warn(player, "energy_needs_single_item");
            return;
        }
        if (next.generatedItem() != Items.AIR && getStoredCount() > 0 &&
            !PylonItemRules.exactlyEqual(getStoredItem(), new ItemStack(next.generatedItem()))) {
            warn(player, "upgrade_incompatible_items");
            return;
        }
        held.shrink(1);
        upgrades.put(next.slot(), next);
        warnedPlayer = null;
        if (next.isTank() || next.isEnergy()) capabilitiesChanged();
        sync();
        if (previous != null) giveUpgrade(player, previous);
    }

    private boolean tryRemoveUpgrade(Player player) {
        PylonUpgrade upgrade = upgrades.values().stream().findFirst().orElse(null);
        if (upgrade == null) return false;
        if (!(level instanceof ServerLevel)) return true;

        if (upgrade == PylonUpgrade.FLUID_STORAGE && getFluidAmount() > 0) {
            warn(player, "tank_needs_empty");
            return true;
        }
        if ((upgrade == PylonUpgrade.XP_TANK && getExperience() > 0) ||
            (upgrade.isEnergy() && getEnergy() > 0) ||
            ((upgrade == PylonUpgrade.HARVESTER || upgrade == PylonUpgrade.SMELT) && !outputBuffer.isEmpty())) {
            warn(player, "specialty_needs_empty");
            return true;
        }
        if (upgrade.slot() == PylonUpgrade.Slot.STORAGE &&
            (getStoredCount() > PylonContents.CAPACITY || getFluidAmount() > BASE_FLUID_CAPACITY_MB)) {
            warn(player, "storage_over_capacity");
            return true;
        }
        if (upgrade.generatesFluid() && getFluidAmount() > 0) {
            long now = level.getGameTime();
            if (!player.getUUID().equals(warnedPlayer) || now >= warningExpires) {
                warnedPlayer = player.getUUID();
                warningExpires = now + CONFIRM_TICKS;
                warn(player, "confirm_fluid_void");
                return true;
            }
            clearFluid();
        }
        upgrades.remove(upgrade.slot());
        if (upgrade == PylonUpgrade.FILTER) {
            filter.clearContent();
            allowList = false;
        }
        if (upgrade == PylonUpgrade.SMELT) smeltFuel = 0;
        warnedPlayer = null;
        if (upgrade.isTank() || upgrade.isEnergy()) capabilitiesChanged();
        sync();
        giveUpgrade(player, upgrade);
        return true;
    }

    private void warn(Player player, String key) {
        notifyPlayer(player, Component.translatable("message.gash_super_pylons." + key));
    }

    private static void giveUpgrade(Player player, PylonUpgrade upgrade) {
        player.getInventory().placeItemBackInInventory(upgradeStack(upgrade));
    }

    private static ItemStack upgradeStack(PylonUpgrade upgrade) {
        return new ItemStack(SuperPylonsMod.UPGRADES.get(upgrade).get());
    }

    public void withdraw(Player player) {
        if (!(level instanceof ServerLevel) || player.isSpectator()) return;
        ItemStack removed = contents.extract(player.isShiftKeyDown() ? Integer.MAX_VALUE : 1);
        if (removed.isEmpty()) return;
        sync();
        player.getInventory().placeItemBackInInventory(removed);
    }

    public void tick(ServerLevel server) {
        PylonUpgrade specialty = getSpecialty();
        if (specialty == null) return;
        PylonUpgrade speed = upgrades.get(PylonUpgrade.Slot.SPEED);
        int speedTier = speed == null ? 0 : speed.tier();
        if (specialty == PylonUpgrade.XP_TANK) {
            transferExperience(server, 1 << speedTier);
            return;
        }
        if (specialty.isEnergy()) {
            transferEnergy(server);
            if (energyUpdatePending && Math.floorMod(server.getGameTime(), 10) == Math.floorMod(worldPosition.hashCode(), 10)) {
                energyUpdatePending = false;
                sync();
            }
            return;
        }
        int interval = speed == null ? 40 : speed.value();
        if (Math.floorMod(server.getGameTime(), interval) != Math.floorMod(worldPosition.hashCode(), interval)) return;

        PylonUpgrade quantity = upgrades.get(PylonUpgrade.Slot.QUANTITY);
        if (specialty == PylonUpgrade.FERTILIZER) {
            fertilize(server, Math.max(1, speedTier), 1 << (quantity == null ? 0 : quantity.tier() - 1));
            return;
        }
        if (specialty == PylonUpgrade.HARVESTER) {
            harvest(server);
            return;
        }
        if (specialty == PylonUpgrade.CLICKER) {
            clickBelow(server);
            return;
        }
        int amount = quantity == null ? 1 : quantity.value();
        if (specialty == PylonUpgrade.MAGNET) {
            collectItems(server, amount);
            return;
        }
        if (specialty == PylonUpgrade.SMELT) {
            smelt(server);
            return;
        }
        if (specialty.generatesFluid()) {
            generateFluid(specialty.generatedFluid(), amount * 100);
        } else if (specialty.generatedItem() != Items.AIR && getStoredCount() < getItemCapacity()) {
            if (contents.add(new ItemStack(specialty.generatedItem()), amount, getItemCapacity()) > 0) sync();
        }
    }

    private void collectItems(ServerLevel server, int maximum) {
        if (getStoredCount() >= getItemCapacity()) return;
        int radius = range();
        AABB area = new AABB(worldPosition).inflate(radius, VERTICAL_RANGE, radius);
        for (ItemEntity entity : server.getEntitiesOfClass(ItemEntity.class, area,
                item -> item.isAlive() && area.contains(item.position()))) {
            ItemStack stack = entity.getItem();
            ItemStack sample = stack.copyWithCount(Math.min(maximum, stack.getCount()));
            int inserted = insertFrom(sample);
            if (inserted == 0) continue;
            if (inserted == stack.getCount()) entity.discard();
            else entity.setItem(stack.copyWithCount(stack.getCount() - inserted));
            maximum -= inserted;
            if (maximum == 0 || getStoredCount() >= getItemCapacity()) return;
        }
    }

    private void smelt(ServerLevel server) {
        var output = outputBelow(server);
        if (output == null) return;
        flushOutput(output);
        if (!outputBuffer.isEmpty() || getStoredCount() == 0) return;
        ItemStack input = getStoredItem();
        SmeltingResult recipe = smeltingRecipe(server, input);
        if (recipe == null || recipe.output().isEmpty() || !output.apply(recipe.output().copy(), true).isEmpty()) return;
        int cost = Math.max(1, recipe.fuelTicks());
        if (smeltFuel >= cost) {
            smeltFuel -= cost;
        } else if (!consumeSmeltingEnergy(server)) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos source = worldPosition.relative(direction);
                if (!server.hasChunkAt(source)) continue;
                int taken = takeSmeltingFuel(server, source, direction.getOpposite());
                if (taken <= 0) continue;
                smeltFuel += taken;
                setChanged();
                break;
            }
            if (smeltFuel < cost) return;
            smeltFuel -= cost;
        }
        if (isRemoved() || !PylonItemRules.exactlyEqual(input, getStoredItem())) return;
        contents.extract(1);
        outputBuffer.add(recipe.output());
        sync();
        flushOutput(output);
    }

    private boolean consumeSmeltingEnergy(ServerLevel server) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos source = worldPosition.relative(direction);
            if (server.hasChunkAt(source) && server.getBlockEntity(source) instanceof PylonBlockEntityBase pylon &&
                pylon.energyLimit(false, SMELT_ENERGY_COST) == SMELT_ENERGY_COST) {
                pylon.energy.transfer(false, SMELT_ENERGY_COST, server.getGameTime());
                pylon.energyChanged();
                return true;
            }
        }
        return false;
    }

    private void transferExperience(ServerLevel server, int levels) {
        long before = experience;
        AABB block = new AABB(worldPosition);
        Vec3 center = block.getCenter();
        for (ExperienceOrb orb : server.getEntitiesOfClass(ExperienceOrb.class, block.inflate(2),
                orb -> orb.isAlive() && block.distanceToSqr(orb.position()) <= 4)) {
            long points = orbExperience(orb);
            if (points > 0 && points <= Long.MAX_VALUE - experience) {
                experience += points;
                orb.discard();
            }
        }
        for (ServerPlayer player : playersInColumn(server, block, center.y + 0.5, center.y + 1.5)) {
            experience += PylonExperience.collect(player, levels, Long.MAX_VALUE - experience);
        }
        if (experience > 0) {
            for (ServerPlayer player : playersInColumn(server, block, center.y - 2.5, center.y - 0.5)) {
                experience -= PylonExperience.dispense(player, levels, experience);
            }
        }
        if (experience != before) sync();
    }

    private static List<ServerPlayer> playersInColumn(ServerLevel server, AABB block, double minY, double maxY) {
        AABB column = new AABB(block.minX, minY, block.minZ, block.maxX, maxY, block.maxZ);
        return server.getEntitiesOfClass(ServerPlayer.class, column,
            player -> player.isAlive() && !player.isSpectator() && column.contains(player.position()));
    }

    private int range() {
        PylonUpgrade range = upgrades.get(PylonUpgrade.Slot.RANGE);
        return range == null ? 1 : range.value();
    }

    private Iterable<BlockPos> farmArea() {
        int radius = range();
        return BlockPos.betweenClosed(worldPosition.offset(-radius, -VERTICAL_RANGE, -radius),
            worldPosition.offset(radius, VERTICAL_RANGE, radius));
    }

    private void fertilize(ServerLevel server, int ticks, int count) {
        List<BlockPos> plants = new ArrayList<>();
        for (BlockPos pos : farmArea()) {
            if (server.hasChunkAt(pos) && PylonFarming.canGrow(server.getBlockState(pos))) plants.add(pos.immutable());
        }
        ServerPlayer automation = getAutomationPlayer(server, AUTOMATION_PROFILE);
        RandomSource random = server.getRandom();
        for (int i = plants.size() - 1; i >= 0 && count > 0; i--) {
            int index = random.nextInt(i + 1);
            BlockPos pos = plants.set(index, plants.get(i));
            if (!mayWorkAt(server, automation, pos)) continue;
            count--;
            for (int j = 0; j < ticks; j++) {
                BlockState plant = server.getBlockState(pos);
                if (!PylonFarming.canGrow(plant)) break;
                plant.randomTick(server, pos, random);
            }
        }
    }

    private void harvest(ServerLevel server) {
        BiFunction<ItemStack, Boolean, ItemStack> output = outputBelow(server);
        if (output == null) return;
        flushOutput(output);
        if (!outputBuffer.isEmpty()) return;
        ServerPlayer automation = getAutomationPlayer(server, AUTOMATION_PROFILE);
        for (BlockPos pos : farmArea()) {
            if (!server.hasChunkAt(pos)) continue;
            BlockState state = server.getBlockState(pos);
            PylonFarming.Harvest harvest = PylonFarming.harvest(server, pos, state);
            if (harvest == null || !mayWorkAt(server, automation, pos)) continue;
            boolean canInsert = harvest.drops().stream()
                .anyMatch(drop -> output.apply(drop.copy(), true).getCount() < drop.getCount());
            if (!canInsert || !server.setBlockAndUpdate(pos, harvest.remainder())) continue;
            outputBuffer.addAll(harvest.drops());
            setChanged();
            flushOutput(output);
            return;
        }
    }

    private BiFunction<ItemStack, Boolean, ItemStack> outputBelow(ServerLevel server) {
        BlockPos target = worldPosition.below();
        return server.hasChunkAt(target) ? inventoryOutput(server, target) : null;
    }

    private void flushOutput(BiFunction<ItemStack, Boolean, ItemStack> output) {
        for (Iterator<ItemStack> it = outputBuffer.iterator(); it.hasNext();) {
            ItemStack stack = it.next();
            ItemStack remaining = output.apply(stack.copy(), false);
            if (remaining.getCount() == stack.getCount()) continue;
            stack.setCount(remaining.getCount());
            if (stack.isEmpty()) it.remove();
            setChanged();
        }
    }

    private static boolean mayWorkAt(ServerLevel server, ServerPlayer automation, BlockPos pos) {
        return server.getWorldBorder().isWithinBounds(pos) && server.mayInteract(automation, pos) &&
            !server.getServer().isUnderSpawnProtection(server, pos, automation);
    }

    private void clickBelow(ServerLevel server) {
        if (contents.count() == 0) return;
        BlockPos target = worldPosition.below();
        if (!server.hasChunkAt(target) || server.getBlockState(target).isAir() ||
            !server.getWorldBorder().isWithinBounds(target)) return;
        ServerPlayer clicker = getAutomationPlayer(server, AUTOMATION_PROFILE);
        if (!mayWorkAt(server, clicker, target)) return;

        Vec3 hit = Vec3.atCenterOf(target).add(0, 0.5, 0);
        clicker.setPos(hit.x, hit.y, hit.z);
        clicker.setXRot(90);
        clicker.setYRot(0);
        ItemStack original = contents.displayItem();
        clicker.setItemInHand(InteractionHand.MAIN_HAND, contents.extract(1));
        try {
            clicker.gameMode.useItemOn(clicker, server, clicker.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(hit, Direction.UP, target, false));
        } finally {
            ItemStack remaining = clicker.getMainHandItem();
            boolean changed = remaining.getCount() != 1 || !PylonItemRules.exactlyEqual(original, remaining);
            clicker.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            int kept = isRemoved() ? 0 : contents.add(remaining, remaining.getCount(), getItemCapacity());
            remaining.shrink(kept);
            if (!remaining.isEmpty()) Block.popResource(server, worldPosition, remaining);
            for (int slot = 0; slot < clicker.getInventory().getContainerSize(); slot++) {
                ItemStack extra = clicker.getInventory().removeItemNoUpdate(slot);
                if (!extra.isEmpty()) Block.popResource(server, worldPosition, extra);
            }
            if (changed && !isRemoved()) sync();
        }
    }

    protected String saveUpgrades() {
        return upgrades.values().stream().map(PylonUpgrade::id).collect(Collectors.joining(","));
    }

    protected void loadUpgrades(String saved) {
        upgrades.clear();
        for (String id : saved.split(",")) {
            PylonUpgrade upgrade = PylonUpgrade.byId(id);
            if (upgrade != null) upgrades.put(upgrade.slot(), upgrade);
        }
    }

    public boolean hasPackedDrop() {
        return packedDrop || experience > 0 || energy.amount() > 0 || (getStoredCount() > 0 &&
            getStoredCount() > (long) MAX_LOOSE_STACKS * getStoredItem().getMaxStackSize());
    }

    public void dropContents() {
        if (!(level instanceof ServerLevel) || contentsDropped) return;
        contentsDropped = true;
        clearFluid();
        for (ItemStack output : outputBuffer) Block.popResource(level, worldPosition, output);
        outputBuffer.clear();
        packedDrop = hasPackedDrop();
        if (packedDrop) {
            Block.popResource(level, worldPosition, createPackedDrop());
            contents.restore(ItemStack.EMPTY, 0);
        } else {
            while (contents.count() > 0) {
                Block.popResource(level, worldPosition, contents.extract(Integer.MAX_VALUE));
            }
            for (PylonUpgrade upgrade : upgrades.values()) {
                Block.popResource(level, worldPosition, upgradeStack(upgrade));
            }
        }
        upgrades.clear();
    }

    protected void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
}
