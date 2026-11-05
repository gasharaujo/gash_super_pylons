package net.gasharaujo.superpylons.block;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public abstract class PylonBlockEntityBase extends BlockEntity implements PylonExperienceStorage {
    private static final GameProfile AUTOMATION_PROFILE =
        new GameProfile(UUID.fromString("507ad2bf-5600-42ed-9fc4-4b7076fc34b5"), "[Super Pylon]");
    private static final int BASE_FLUID_CAPACITY_MB = 32_000;
    private static final int BASE_XP_CAPACITY = 32_000;
    private static final int CONFIRM_TICKS = 100;
    private static final int MAX_LOOSE_STACKS = 64;
    private static final int SMELT_ENERGY_COST = 1000;
    private static final int FURNACE_TICKS_PER_ITEM = 200;
    private static final int XP_PER_BOTTLE = 7;
    static final int FILTER_SIZE = 27;

    protected final PylonContents contents = new PylonContents();
    protected final PylonEnergy energy = new PylonEnergy();
    protected final List<ItemStack> outputBuffer = new ArrayList<>();
    protected final SimpleContainer filter = new SimpleContainer(FILTER_SIZE);
    private final String[] filterTags = new String[FILTER_SIZE];
    protected boolean allowList;
    protected long smeltFuel;
    private Block paint = Blocks.IRON_BLOCK;
    private Direction workDirection = Direction.DOWN;
    private int forcedRadius;
    private UUID wireId = UUID.randomUUID();
    private final List<PylonWire> wires = new ArrayList<>();
    private ItemStack breakTool = ItemStack.EMPTY;
    private final List<Auxiliary> auxiliaries = new ArrayList<>(2);
    private Auxiliary workshopOwner;
    private final List<ItemStack> shelves = new ArrayList<>();
    private String workshopStatus = "connections";
    private long workshopCost;
    private final EnumMap<PylonUpgrade.Slot, PylonUpgrade> upgrades = new EnumMap<>(PylonUpgrade.Slot.class);
    private long experience;
    private int experienceRemainder;
    private String experienceFluid = "";
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
        if (isWorkshop()) return 1;
        return PylonUpgrade.itemCapacity(upgrades.get(PylonUpgrade.Slot.STORAGE));
    }

    public PylonUpgrade getGenerator() { return upgrades.get(PylonUpgrade.Slot.GENERATOR); }
    public PylonUpgrade getSpecialty() { return upgrades.get(PylonUpgrade.Slot.SPECIALTY); }
    public PylonUpgrade getProcessing() { return upgrades.get(PylonUpgrade.Slot.PROCESSING); }
    public double getFuelUses() { return (double) smeltFuel / FURNACE_TICKS_PER_ITEM; }
    public boolean hasSuperUpgrade() { return upgrades.containsKey(PylonUpgrade.Slot.SUPER); }

    public List<ItemStack> getShelfItems() { return shelves.stream().map(ItemStack::copy).toList(); }
    public String getWorkshopStatus() { return workshopStatus; }
    public long getWorkshopCost() { return workshopCost; }
    public int getEnchantmentPower() {
        PylonUpgrade capacity = upgrades.get(PylonUpgrade.Slot.STORAGE);
        return capacity == null ? 1 : capacity.tier() == 6 ? 100 : capacity.tier() * 10;
    }
    public int getMinimumEnchantments() {
        PylonUpgrade quantity = upgrades.get(PylonUpgrade.Slot.QUANTITY);
        return quantity == null ? 1 : quantity.tier() + 1;
    }

    protected void loadWorkshopStatus(String status, long cost) {
        workshopStatus = List.of("connections", "input", "lapis", "second_item", "no_result", "xp", "complete").contains(status) ? status : "connections";
        workshopCost = Math.max(0, cost);
    }

    protected void loadShelfItems(List<ItemStack> saved) {
        shelves.clear();
        if (getSpecialty() != PylonUpgrade.ENCHANTING) return;
        for (ItemStack shelf : saved) {
            if (shelves.size() == 64) break;
            if (PylonShelfEffects.isShelf(shelf)) shelves.add(shelf.copyWithCount(1));
        }
    }

    public int dropDistance() {
        PylonUpgrade range = upgrades.get(PylonUpgrade.Slot.RANGE);
        return range == null ? 1 : range.tier() + 1;
    }

    public List<PylonUpgrade> getInstalledUpgrades() {
        return List.copyOf(upgrades.values());
    }

    public ItemStack getBreakTool() { return breakTool.copy(); }

    protected void loadBreakTool(ItemStack tool) {
        breakTool = getSpecialty() == PylonUpgrade.BREAK && PylonItemRules.isMiningTool(tool) ?
            tool.copyWithCount(1) : ItemStack.EMPTY;
    }

    public Block framePaint() { return paint; }
    public List<PylonWire> outgoingWires() { return List.copyOf(wires); }

    public Direction workDirection() { return workDirection; }

    void setWorkDirection(Direction direction) {
        if (direction == workDirection) return;
        workDirection = direction;
        capabilitiesChanged();
        sync();
    }

    protected void loadWorkDirection(String value) {
        workDirection = Direction.DOWN;
        for (Direction direction : Direction.values()) {
            if (direction.getSerializedName().equals(value)) workDirection = direction;
        }
    }

    protected BlockPos workTarget() { return worldPosition.relative(workDirection); }

    protected int forcedRadius() { return forcedRadius; }

    protected void loadForcedRadius(int radius) { forcedRadius = Math.max(0, Math.min(8, radius)); }

    protected abstract void forceChunk(ServerLevel server, int x, int z, boolean add);

    private void updateChunkLoading() {
        if (level instanceof ServerLevel server) {
            updateChunkLoading(server, upgrades.containsKey(PylonUpgrade.Slot.CHUNK_LOADER) ? range() : 0);
        }
    }

    private void updateChunkLoading(ServerLevel server, int radius) {
        if (radius == forcedRadius) return;
        int edge = Math.max(radius, forcedRadius);
        int chunkX = worldPosition.getX() >> 4;
        int chunkZ = worldPosition.getZ() >> 4;
        for (int x = -edge; x <= edge; x++) {
            for (int z = -edge; z <= edge; z++) {
                int distance = Math.max(Math.abs(x), Math.abs(z));
                boolean previous = forcedRadius > 0 && distance <= forcedRadius;
                boolean next = radius > 0 && distance <= radius;
                if (previous != next) forceChunk(server, chunkX + x, chunkZ + z, next);
            }
        }
        forcedRadius = radius;
        setChanged();
    }
    UUID wireId() { return wireId; }

    private record Auxiliary(BlockPos pos, UUID id) {}

    public boolean isWorkshop() {
        return getSpecialty() == PylonUpgrade.ANVIL || getSpecialty() == PylonUpgrade.ENCHANTING;
    }

    boolean linkAuxiliary(PylonBlockEntityBase target, Player player) {
        if (!isWorkshop() || target.isWorkshop() || !validAuxiliaryPosition(target.worldPosition)) {
            warn(player, "workshop_invalid");
            return false;
        }
        Auxiliary link = new Auxiliary(target.worldPosition, target.wireId);
        int maximum = getSpecialty() == PylonUpgrade.ENCHANTING && hasSuperUpgrade() ? 3 : 2;
        if (auxiliaries.contains(link)) return auxiliaries.size() >= maximum;
        int vacant = -1;
        for (int i = 0; i < Math.min(maximum, auxiliaries.size()); i++) {
            Auxiliary old = auxiliaries.get(i);
            if (level.hasChunkAt(old.pos()) && auxiliary(old) == null) { vacant = i; break; }
        }
        if ((auxiliaries.size() >= maximum && vacant < 0) || target.workshop() != null) {
            warn(player, "workshop_full");
            return false;
        }
        if (vacant >= 0) auxiliaries.set(vacant, link);
        else auxiliaries.add(link);
        target.workshopOwner = new Auxiliary(worldPosition, wireId);
        target.sync();
        warn(player, "workshop_linked");
        sync();
        return auxiliaries.size() == maximum;
    }

    private boolean validAuxiliaryPosition(BlockPos pos) {
        double distance = worldPosition.distSqr(pos);
        return distance >= 1 && distance <= 25;
    }

    private PylonBlockEntityBase auxiliary(Auxiliary link) {
        if (level.hasChunkAt(link.pos()) && validAuxiliaryPosition(link.pos()) &&
            level.getBlockEntity(link.pos()) instanceof PylonBlockEntityBase target && link.id().equals(target.wireId) &&
            !target.isWorkshop()) return target;
        return null;
    }

    private PylonBlockEntityBase workshop() {
        if (level == null || isWorkshop() || workshopOwner == null || !level.hasChunkAt(workshopOwner.pos())) return null;
        Auxiliary self = new Auxiliary(worldPosition, wireId);
        if (level.getBlockEntity(workshopOwner.pos()) instanceof PylonBlockEntityBase candidate &&
            candidate.wireId.equals(workshopOwner.id()) && candidate.isWorkshop() &&
            candidate.auxiliaries.contains(self) && candidate.validAuxiliaryPosition(worldPosition)) {
            return candidate;
        }
        return null;
    }

    public PylonUpgrade auxiliaryMode() {
        PylonBlockEntityBase source = workshop();
        if (source != null && source.auxiliaries.indexOf(new Auxiliary(worldPosition, wireId)) == 2) return null;
        return source == null ? null : source.getSpecialty();
    }

    private void refreshWorkshopLinks() {
        for (Auxiliary link : auxiliaries) {
            PylonBlockEntityBase target = auxiliary(link);
            Auxiliary owner = new Auxiliary(worldPosition, wireId);
            if (target != null && !owner.equals(target.workshopOwner)) {
                target.workshopOwner = owner;
                target.sync();
            }
        }
    }

    private void clearWorkshopLinks() {
        for (Auxiliary link : auxiliaries) {
            PylonBlockEntityBase target = auxiliary(link);
            if (target != null && new Auxiliary(worldPosition, wireId).equals(target.workshopOwner)) {
                target.workshopOwner = null;
                target.sync();
            }
        }
        auxiliaries.clear();
    }

    private void trimWorkshopLinks() {
        int maximum = getSpecialty() == PylonUpgrade.ENCHANTING && hasSuperUpgrade() ? 3 : 2;
        while (auxiliaries.size() > maximum) {
            Auxiliary link = auxiliaries.remove(auxiliaries.size() - 1);
            PylonBlockEntityBase target = auxiliary(link);
            if (target != null && new Auxiliary(worldPosition, wireId).equals(target.workshopOwner)) {
                target.workshopOwner = null;
                target.sync();
            }
        }
    }

    protected String saveWorkshopOwner() {
        return workshopOwner == null ? "" : workshopOwner.pos().asLong() + ":" + workshopOwner.id();
    }

    protected void loadWorkshopOwner(String value) {
        workshopOwner = null;
        String[] parts = value.split(":");
        if (parts.length != 2) return;
        try {
            workshopOwner = new Auxiliary(BlockPos.of(Long.parseLong(parts[0])), UUID.fromString(parts[1]));
        } catch (IllegalArgumentException ignored) {
            // Ignore malformed workshop owners.
        }
    }

    private void emitEnchantingParticles(ServerLevel server) {
        if (auxiliaries.size() < 2 || server.getGameTime() % 4 != 0) return;
        PylonBlockEntityBase first = auxiliary(auxiliaries.get(0));
        PylonBlockEntityBase second = auxiliary(auxiliaries.get(1));
        if (first == null || second == null || !(first.suppliesLapis() && second.experience > 0 ||
            second.suppliesLapis() && first.experience > 0)) return;
        for (Auxiliary link : auxiliaries.subList(0, 2)) {
            double x = link.pos().getX() - worldPosition.getX();
            double y = link.pos().getY() - worldPosition.getY() + .1;
            double z = link.pos().getZ() - worldPosition.getZ();
            server.sendParticles(ParticleTypes.ENCHANT, worldPosition.getX() + .5, worldPosition.getY() + .67,
                worldPosition.getZ() + .5, 0, x, y, z, 1);
        }
    }

    private boolean suppliesLapis() { return getStoredCount() > 0 && getStoredItem().is(Items.LAPIS_LAZULI); }

    private void workshopStatus(String status, long cost) {
        if (status.equals(workshopStatus) && cost == workshopCost) return;
        workshopStatus = status;
        workshopCost = cost;
        sync();
    }

    private void operateWorkshop(ServerLevel server) {
        if (auxiliaries.size() < 2) { workshopStatus("connections", 0); return; }
        PylonBlockEntityBase first = auxiliary(auxiliaries.get(0));
        PylonBlockEntityBase second = auxiliary(auxiliaries.get(1));
        if (first == null || second == null || first.isXpTank() == second.isXpTank()) {
            workshopStatus("connections", 0);
            return;
        }
        PylonBlockEntityBase xp = first.isXpTank() ? first : second;
        PylonBlockEntityBase material = first.isXpTank() ? second : first;
        if (getStoredCount() == 0) { workshopStatus("input", 0); return; }
        boolean enchanting = getSpecialty() == PylonUpgrade.ENCHANTING;
        if (enchanting ? !material.suppliesLapis() : material.getStoredCount() == 0) {
            workshopStatus(enchanting ? "lapis" : "second_item", 0);
            return;
        }
        ItemStack blacklist = ItemStack.EMPTY;
        if (enchanting && hasSuperUpgrade() && auxiliaries.size() >= 3) {
            PylonBlockEntityBase third = auxiliary(auxiliaries.get(2));
            if (third == null) { workshopStatus("connections", 0); return; }
            blacklist = third.getStoredItem();
        }
        PylonWorkshopResult result = enchanting ?
            PylonEnchanting.enchant(server, worldPosition, getStoredItem(), getEnchantmentPower(),
                getMinimumEnchantments(), blacklist, shelves) :
            PylonEnchanting.anvil(getAutomationPlayer(server, AUTOMATION_PROFILE), getStoredItem(), material.getStoredItem());
        if (result == null) { workshopStatus("no_result", 0); return; }
        long cost = PylonExperience.pointsAtLevel((int) Math.min(1_000_000, result.levels()));
        if (xp.experience < cost) { workshopStatus("xp", cost); return; }
        contents.restore(result.item(), 1, getItemCapacity());
        xp.experience -= cost;
        xp.sync();
        if (enchanting || !hasSuperUpgrade() || !material.getStoredItem().is(Items.ENCHANTED_BOOK)) {
            material.contents.remove(1);
            material.sync();
        }
        workshopStatus = "complete";
        workshopCost = cost;
        sync();
    }

    protected String saveAuxiliaries() {
        StringJoiner data = new StringJoiner(",");
        for (Auxiliary link : auxiliaries) data.add(link.pos().asLong() + ":" + link.id());
        return data.toString();
    }

    protected void loadAuxiliaries(String saved) {
        auxiliaries.clear();
        if (!isWorkshop()) return;
        for (String entry : saved.split(",")) {
            String[] fields = entry.split(":");
            if (fields.length != 2 || auxiliaries.size() == 3) continue;
            try {
                Auxiliary link = new Auxiliary(BlockPos.of(Long.parseLong(fields[0])), UUID.fromString(fields[1]));
                if (!auxiliaries.contains(link)) auxiliaries.add(link);
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed saved links.
            }
        }
    }

    void paintFrame(Block block) {
        Block next = block == null || block == getBlockState().getBlock() || block.defaultBlockState().isAir() ||
            block.defaultBlockState().getRenderShape() != RenderShape.MODEL ? Blocks.IRON_BLOCK : block;
        if (paint == next) return;
        paint = next;
        updatePaintedState();
        sync();
    }

    private void updatePaintedState() {
        boolean painted = paint != Blocks.IRON_BLOCK;
        BlockState state = getBlockState();
        if (state.getValue(PylonFrame.PAINTED) != painted) {
            level.setBlock(worldPosition, state.setValue(PylonFrame.PAINTED, painted), Block.UPDATE_ALL);
        }
    }

    protected String savePaint() { return paint == Blocks.IRON_BLOCK ? "" : BuiltInRegistries.BLOCK.getKey(paint).toString(); }
    protected String saveWireId() { return wireId.toString(); }

    protected String saveWires() {
        StringJoiner data = new StringJoiner(",");
        for (PylonWire wire : wires) data.add(wire.encode());
        return data.toString();
    }

    protected void loadDecorations(String block, String anchor, String links) {
        paint = block.isEmpty() ? Blocks.IRON_BLOCK : PylonToolItem.blockById(block);
        try {
            if (!anchor.isEmpty()) wireId = UUID.fromString(anchor);
        } catch (IllegalArgumentException ignored) {
            // Keep the local anchor if the saved one is invalid.
        }
        wires.clear();
        for (String entry : links.split(",")) {
            PylonWire wire = PylonWire.decode(entry);
            if (wire != null && validWireDistance(wire.target()) && !wires.contains(wire)) wires.add(wire);
        }
    }

    private boolean validWireDistance(BlockPos target) {
        return !worldPosition.equals(target) && worldPosition.distSqr(target) <= PylonWire.MAX_LENGTH * PylonWire.MAX_LENGTH;
    }

    boolean connectWire(PylonWire.Type type, PylonBlockEntityBase target, Player player) {
        if (!validWireDistance(target.worldPosition)) {
            warn(player, "wire_distance");
            return false;
        }
        if (!supportsTransfer(type, false) || !target.supportsTransfer(type, true)) {
            warn(player, "wire_incompatible");
            return false;
        }
        wires.add(new PylonWire(type, target.worldPosition, target.wireId));
        target.setChanged();
        warn(player, "wire_connected");
        sync();
        return true;
    }

    boolean disconnectWire(PylonBlockEntityBase target, Player player) {
        boolean removedOutgoing = removeWiresTo(target, player);
        boolean removedIncoming = target.removeWiresTo(this, player);
        if (!removedOutgoing && !removedIncoming) return false;
        warn(player, "wire_removed");
        return true;
    }

    private boolean removeWiresTo(PylonBlockEntityBase target, Player player) {
        boolean removed = false;
        for (Iterator<PylonWire> iterator = wires.iterator(); iterator.hasNext();) {
            PylonWire wire = iterator.next();
            if (wire.target().equals(target.worldPosition) && wire.targetId().equals(target.wireId)) {
                iterator.remove();
                player.getInventory().placeItemBackInInventory(wire.type().itemStack());
                removed = true;
            }
        }
        if (removed) sync();
        return removed;
    }

    private boolean supportsTransfer(PylonWire.Type type, boolean input) {
        if (isVoid()) return input;
        return switch (type) {
            case ENERGY -> isEnergy();
            case FLUID -> isTank();
            case ITEM -> input ? hasItemStorage() : canExtractItems();
            case XP -> getSpecialty() == PylonUpgrade.XP_TANK;
        };
    }

    public boolean hasFilter() { return upgrades.containsKey(PylonUpgrade.Slot.FILTER); }

    protected NonNullList<ItemStack> filterItems() {
        NonNullList<ItemStack> entries = NonNullList.withSize(FILTER_SIZE, ItemStack.EMPTY);
        for (int i = 0; i < FILTER_SIZE; i++) entries.set(i, filter.getItem(i));
        return entries;
    }

    protected void loadFilter(NonNullList<ItemStack> entries, boolean allowList) {
        clearFilter();
        this.allowList = hasFilter() && allowList;
        if (!hasFilter()) return;
        for (int i = 0; i < FILTER_SIZE; i++) filter.setItem(i, entries.get(i).copyWithCount(1));
    }

    String filterTag(int slot) { return filterTags[slot]; }

    void setFilterTag(int slot, String tag) {
        filterTags[slot] = tag;
        setChanged();
    }

    protected String saveFilterTags() {
        var saved = new StringJoiner(",");
        for (int i = 0; i < FILTER_SIZE; i++) {
            if (filterTags[i] != null) saved.add(i + "=" + filterTags[i]);
        }
        return saved.toString();
    }

    protected void loadFilterTags(String saved) {
        if (!hasFilter()) return;
        for (String entry : saved.split(",")) {
            String[] pair = entry.split("=", 2);
            if (pair.length != 2) continue;
            try {
                int slot = Integer.parseInt(pair[0]);
                if (slot >= 0 && slot < FILTER_SIZE && PylonItemRules.tags(filter.getItem(slot)).anyMatch(pair[1]::equals)) {
                    filterTags[slot] = pair[1];
                }
            } catch (NumberFormatException ignored) {
                // Skip malformed filter slots.
            }
        }
    }

    private void clearFilter() {
        filter.clearContent();
        Arrays.fill(filterTags, null);
    }

    public long getExperience() { return experience; }

    public long getExperienceCapacity() {
        if (isVoid()) return Long.MAX_VALUE;
        return getSpecialty() == PylonUpgrade.XP_TANK ?
            PylonUpgrade.storageCapacity(BASE_XP_CAPACITY, upgrades.get(PylonUpgrade.Slot.STORAGE)) : 0;
    }

    public boolean isXpTank() { return getSpecialty() == PylonUpgrade.XP_TANK; }
    public boolean isVoid() { return getSpecialty() == PylonUpgrade.VOID; }

    private boolean hasExperience() { return experience > 0 || experienceRemainder > 0; }

    private long experienceSpace() { return Math.max(0, getExperienceCapacity() - experience); }

    private long wholeExperienceSpace() {
        return Math.max(0, experienceSpace() - (experienceRemainder > 0 ? 1 : 0));
    }

    protected record XpFluidState(long points, int remainder, String fluidId) {}

    protected XpFluidState xpFluidSnapshot() { return new XpFluidState(experience, experienceRemainder, experienceFluid); }

    protected void restoreXpFluid(XpFluidState state) {
        experience = state.points();
        experienceRemainder = state.remainder();
        experienceFluid = state.fluidId();
    }

    protected void loadXpFluid(int remainder, String fluidId) {
        experienceRemainder = isXpTank() ? Math.max(0, Math.min(PylonXpFluids.MILLIBUCKETS_PER_POINT - 1, remainder)) : 0;
        experienceFluid = isXpTank() ? fluidId : "";
    }

    protected Fluid preferredXpFluid() {
        var fluids = PylonXpFluids.available();
        for (Fluid fluid : fluids) if (PylonXpFluids.id(fluid).equals(experienceFluid)) return fluid;
        return fluids.isEmpty() ? Fluids.EMPTY : fluids.get(0);
    }

    protected long xpFluidCapacity() { return PylonXpFluids.millibuckets(getExperienceCapacity()); }

    protected long xpFluidAmount() {
        long whole = PylonXpFluids.millibuckets(experience);
        return whole > Long.MAX_VALUE - experienceRemainder ? Long.MAX_VALUE : whole + experienceRemainder;
    }

    protected int xpFluidLimit(boolean input, Fluid fluid, int requested) {
        if (!(level instanceof ServerLevel) || isRemoved() || !isXpTank() || requested <= 0 || !PylonXpFluids.supports(fluid)) return 0;
        long available = input ? Math.max(0, PylonXpFluids.millibuckets(experienceSpace()) - experienceRemainder) : xpFluidAmount();
        return (int) Math.min(requested, available);
    }

    protected void moveXpFluid(boolean input, Fluid fluid, int amount) {
        int ratio = PylonXpFluids.MILLIBUCKETS_PER_POINT;
        if (input) {
            if (!hasExperience() || experienceFluid.isEmpty()) experienceFluid = PylonXpFluids.id(fluid);
            int remainder = experienceRemainder + amount % ratio;
            experience += amount / ratio + remainder / ratio;
            experienceRemainder = remainder % ratio;
        } else {
            int remainder = experienceRemainder - amount % ratio;
            experience -= amount / ratio + (remainder < 0 ? 1 : 0);
            experienceRemainder = Math.floorMod(remainder, ratio);
        }
    }

    @Override
    public long receiveExperience(long points, boolean simulate) {
        if (!(level instanceof ServerLevel) || isRemoved() || points <= 0) return 0;
        if (isVoid()) return points;
        long accepted = Math.min(points, wholeExperienceSpace());
        if (!simulate && accepted > 0) {
            experience += accepted;
            sync();
        }
        return accepted;
    }

    @Override
    public long extractExperience(long points, boolean simulate) {
        if (!(level instanceof ServerLevel) || isRemoved() || points <= 0) return 0;
        long taken = Math.min(points, experience);
        if (!simulate && taken > 0) {
            experience -= taken;
            sync();
        }
        return taken;
    }

    public int comparatorSignal() {
        long amount = getStoredCount();
        long capacity = getItemCapacity();
        if (isEnergy()) {
            amount = getEnergy();
            capacity = getEnergyCapacity();
        } else if (isTank()) {
            amount = getFluidAmount();
            capacity = getFluidCapacity();
        } else if (getSpecialty() == PylonUpgrade.XP_TANK) {
            amount = xpFluidAmount();
            capacity = xpFluidCapacity();
        }
        return amount == 0 || capacity == 0 ? 0 : 1 + (int) Math.floor(14 * Math.min(1, (double) amount / capacity));
    }
    public boolean isEnergy() { return upgrades.containsKey(PylonUpgrade.Slot.ENERGY); }
    public int getEnergy() { return energy.amount(); }

    public int getEnergyCapacity() {
        if (isVoid()) return Integer.MAX_VALUE;
        PylonUpgrade upgrade = upgrades.get(PylonUpgrade.Slot.ENERGY);
        return upgrade == null ? 0 : upgrade.value();
    }

    protected void loadSpecialtyAmounts(long savedExperience, int savedEnergy) {
        experience = getSpecialty() == PylonUpgrade.XP_TANK ? Math.max(0, savedExperience) : 0;
        energy.restore(new PylonEnergy.State(Math.max(0, Math.min(getEnergyCapacity(), savedEnergy)),
            Long.MIN_VALUE, 0, 0));
    }

    protected int energyLimit(boolean input, int requested) {
        if (!(level instanceof ServerLevel) || isRemoved() || requested <= 0) return 0;
        if (isVoid()) return input ? requested : 0;
        if (!isEnergy()) return 0;
        return energy.limit(input, requested, getEnergyCapacity(), upgrades.get(PylonUpgrade.Slot.ENERGY).energyTransfer(),
            level.getGameTime());
    }

    protected void energyChanged() {
        setChanged();
        energyUpdatePending = true;
    }

    public boolean isTank() {
        PylonUpgrade generator = getGenerator();
        return getSpecialty() == PylonUpgrade.FLUID_STORAGE || generator != null && generator.generatesFluid();
    }

    public long getFluidCapacity() {
        if (isVoid()) return Long.MAX_VALUE;
        return isTank() ? PylonUpgrade.storageCapacity(BASE_FLUID_CAPACITY_MB, upgrades.get(PylonUpgrade.Slot.STORAGE)) : 0;
    }

    public boolean canFillFluid(Fluid fluid) {
        if (isVoid()) return fluid != Fluids.EMPTY;
        PylonUpgrade generator = getGenerator();
        return fluid != Fluids.EMPTY && (getSpecialty() == PylonUpgrade.FLUID_STORAGE ||
            generator != null && generator.generatesFluid() && generator.generatedFluid() == fluid);
    }

    public abstract long getFluidAmount();
    public abstract Component getFluidName();
    protected abstract void clearFluid();
    protected abstract void generateFluid(Fluid fluid, int amount);
    protected abstract boolean interactWithTank(Player player, InteractionHand hand);
    protected abstract void capabilitiesChanged();
    protected abstract ServerPlayer getAutomationPlayer(ServerLevel level, GameProfile profile);
    protected abstract ItemStack createPackedDrop();
    protected abstract void notifyPlayer(Player player, Component message);
    protected abstract long orbExperience(ExperienceOrb orb);
    protected abstract void transferEnergy(ServerLevel server);
    protected abstract BiFunction<ItemStack, Boolean, ItemStack> inventoryOutput(ServerLevel server, BlockPos target);
    protected abstract ItemStack smeltingRecipe(ServerLevel server, ItemStack input);
    protected abstract int takeSmeltingFuel(ServerLevel server, BlockPos pos, Direction side);
    protected abstract long transferFluidTo(PylonBlockEntityBase target, long limit);
    protected abstract long transferAdjacentExternal(ServerLevel server, BlockPos neighbor, PylonWire.Type type,
                                                     boolean importing, long limit);
    protected abstract PylonExperienceStorage experienceAt(ServerLevel server, BlockPos pos);

    public int insertFrom(ItemStack input) {
        int inserted = insertItems(input, input.getCount(), false);
        input.shrink(inserted);
        return inserted;
    }

    protected ItemStack insertAutomation(ItemStack stack, boolean simulate) {
        int inserted = insertItems(stack, stack.getCount(), simulate);
        return inserted == 0 ? stack : stack.copyWithCount(stack.getCount() - inserted);
    }

    protected int insertItems(ItemStack stack, int amount, boolean simulate) {
        if (!(level instanceof ServerLevel) || amount <= 0 || !canStoreItem(stack)) return 0;
        if (isVoid()) return amount;
        int inserted = contents.insertLimit(stack, amount, getItemCapacity());
        if (!simulate && inserted > 0) {
            contents.add(stack, inserted, getItemCapacity());
            sync();
        }
        return inserted;
    }

    protected ItemStack extractAutomation(int amount, boolean simulate) {
        if (!(level instanceof ServerLevel) || !canExtractItems()) return ItemStack.EMPTY;
        if (simulate) return contents.peek(amount);
        ItemStack extracted = contents.extract(amount);
        if (!extracted.isEmpty()) sync();
        return extracted;
    }

    protected boolean hasItemStorage() {
        PylonUpgrade specialty = getSpecialty();
        return !isRemoved() && (specialty == null || !specialty.locksItems());
    }

    protected boolean canExtractItems() {
        return !isRemoved() && !isVoid() && (hasItemStorage() || getStoredCount() > 0);
    }

    protected boolean canStoreItem(ItemStack input) {
        if (isRemoved() || input.isEmpty() || input.getItem() instanceof PylonUpgradeItem || PylonItemRules.isModifier(input) ||
            (!hasItemStorage() && !isVoid()) || !allowsItem(input)) return false;
        if (getSpecialty() == PylonUpgrade.PLANTER) return PylonFarming.canPlant(input);
        if (getSpecialty() == PylonUpgrade.PLACER) return input.getItem() instanceof BlockItem;
        return true;
    }

    private boolean allowsItem(ItemStack input) {
        if (!hasFilter() || input.isEmpty()) return true;
        for (int i = 0; i < FILTER_SIZE; i++) {
            String tag = filterTags[i];
            if (tag == null ? PylonItemRules.exactlyEqual(filter.getItem(i), input) : PylonItemRules.matches(input, "#" + tag)) {
                return allowList;
            }
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
        if (held.getItem() instanceof PylonToolItem tool) return tool.applyTo(this, player, hand);
        if (getSpecialty() == PylonUpgrade.ENCHANTING && PylonShelfEffects.isShelf(held)) {
            if (level instanceof ServerLevel) {
                if (shelves.size() == 64) {
                    warn(player, "shelf_limit");
                } else {
                    shelves.add(held.copyWithCount(1));
                    held.shrink(1);
                    sync();
                }
            }
            return true;
        }
        if (PylonItemRules.isModifier(held)) return false;
        if (level instanceof ServerLevel) {
            if (isXpTank() && (held.is(Items.EXPERIENCE_BOTTLE) || held.is(Items.GLASS_BOTTLE))) {
                interactWithXpBottles(player, held);
            } else if (!isTank() || !interactWithTank(player, hand)) {
                insertFrom(held);
            }
        }
        return true;
    }

    private void interactWithXpBottles(Player player, ItemStack held) {
        Item returned;
        if (held.is(Items.EXPERIENCE_BOTTLE) && wholeExperienceSpace() >= XP_PER_BOTTLE) {
            experience += XP_PER_BOTTLE;
            returned = Items.GLASS_BOTTLE;
        } else if (held.is(Items.GLASS_BOTTLE) && experience >= XP_PER_BOTTLE) {
            experience -= XP_PER_BOTTLE;
            returned = Items.EXPERIENCE_BOTTLE;
        } else {
            return;
        }
        held.shrink(1);
        player.getInventory().placeItemBackInInventory(new ItemStack(returned));
        sync();
    }

    public boolean tryInstallBreakTool(Player player, InteractionHand hand) {
        if (getSpecialty() != PylonUpgrade.BREAK || player.isSpectator() ||
            hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) return false;
        ItemStack held = player.getMainHandItem();
        if (!PylonItemRules.isMiningTool(held)) return false;
        if (level instanceof ServerLevel) {
            ItemStack previous = breakTool;
            breakTool = held.copyWithCount(1);
            held.shrink(1);
            if (!previous.isEmpty()) player.getInventory().placeItemBackInInventory(previous);
            sync();
        }
        return true;
    }

    private void install(Player player, ItemStack held, PylonUpgrade next) {
        PylonUpgrade previous = upgrades.get(next.slot());
        if (previous != null && previous.tier() >= next.tier()) {
            warn(player, "upgrade_slot_occupied");
            return;
        }
        if (next.locksItems() && getStoredCount() > 0) {
            warn(player, "upgrade_needs_empty");
            return;
        }
        if ((next == PylonUpgrade.ANVIL || next == PylonUpgrade.ENCHANTING) && getStoredCount() > 1) {
            warn(player, "workshop_single_item");
            return;
        }
        if (next == PylonUpgrade.VOID && getEnergy() > 0) {
            warn(player, "specialty_needs_empty");
            return;
        }
        held.shrink(1);
        upgrades.put(next.slot(), next);
        updateChunkLoading();
        warnedPlayer = null;
        if (next.isTank() || next.isEnergy() || next == PylonUpgrade.XP_TANK || next == PylonUpgrade.VOID) capabilitiesChanged();
        sync();
        if (previous != null) giveUpgrade(player, previous);
    }

    void applyUpgradeSetup(Player player, String saved) {
        var next = new EnumMap<PylonUpgrade.Slot, PylonUpgrade>(PylonUpgrade.Slot.class);
        for (String id : saved.split(",")) {
            PylonUpgrade upgrade = PylonUpgrade.byId(id);
            if (upgrade != null) next.put(upgrade.slot(), upgrade);
        }
        if (upgrades.equals(next)) return;

        List<Integer> takeSlots = new ArrayList<>();
        var missing = Component.empty();
        for (PylonUpgrade upgrade : next.values()) {
            if (upgrades.get(upgrade.slot()) == upgrade) continue;
            Item required = SuperPylonsMod.UPGRADES.get(upgrade).get();
            int found = -1;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (player.getInventory().getItem(i).is(required)) {
                    found = i;
                    break;
                }
            }
            if (found >= 0) {
                takeSlots.add(found);
            } else {
                if (!missing.getSiblings().isEmpty()) missing.append(", ");
                missing.append(upgradeStack(upgrade).getHoverName());
            }
        }
        if (!missing.getSiblings().isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.gash_super_pylons.setup_missing", missing));
            return;
        }

        PylonUpgrade previousSpecialty = getSpecialty();
        PylonUpgrade specialty = next.get(PylonUpgrade.Slot.SPECIALTY);
        PylonUpgrade generator = next.get(PylonUpgrade.Slot.GENERATOR);
        boolean generatorChanged = getGenerator() != generator;
        PylonUpgrade processor = next.get(PylonUpgrade.Slot.PROCESSING);
        PylonUpgrade storage = next.get(PylonUpgrade.Slot.STORAGE);
        PylonUpgrade energyUpgrade = next.get(PylonUpgrade.Slot.ENERGY);
        boolean energyUpgradeChanged = upgrades.get(PylonUpgrade.Slot.ENERGY) != energyUpgrade;
        boolean nextTank = specialty == PylonUpgrade.FLUID_STORAGE || generator != null && generator.generatesFluid();
        if ((previousSpecialty != specialty && hasExperience()) ||
            (getFluidAmount() > 0 && (!nextTank || generatorChanged && getGenerator() != null && getGenerator().generatesFluid())) ||
            ((previousSpecialty != specialty || getProcessing() != processor) && !outputBuffer.isEmpty())) {
            warn(player, "specialty_needs_empty");
            return;
        }
        int energyCapacity = energyUpgrade == null ? 0 : energyUpgrade.value();
        if ((specialty == PylonUpgrade.VOID && getEnergy() > 0) || getEnergy() > energyCapacity ||
            getStoredCount() > (specialty == PylonUpgrade.ANVIL || specialty == PylonUpgrade.ENCHANTING ?
                1 : PylonUpgrade.itemCapacity(storage)) ||
            (storage != upgrades.get(PylonUpgrade.Slot.STORAGE) &&
                (getFluidAmount() > PylonUpgrade.storageCapacity(BASE_FLUID_CAPACITY_MB, storage) ||
                 xpFluidAmount() > PylonXpFluids.millibuckets(PylonUpgrade.storageCapacity(BASE_XP_CAPACITY, storage))))) {
            warn(player, "storage_over_capacity");
            return;
        }
        if (specialty != null && getStoredCount() > 0 && specialty != previousSpecialty) {
            if (specialty.locksItems()) {
                warn(player, "upgrade_needs_empty");
                return;
            }
        }

        for (int slot : takeSlots) player.getInventory().getItem(slot).shrink(1);
        player.getInventory().setChanged();
        List<PylonUpgrade> returned = upgrades.values().stream()
            .filter(upgrade -> next.get(upgrade.slot()) != upgrade).toList();
        upgrades.clear();
        upgrades.putAll(next);
        trimWorkshopLinks();
        updateChunkLoading();
        if (specialty != PylonUpgrade.BREAK) giveBackBreakTool(player);
        if (previousSpecialty != specialty) {
            clearWorkshopLinks();
            for (ItemStack shelf : shelves) player.getInventory().placeItemBackInInventory(shelf);
            shelves.clear();
        }
        if (!hasFilter()) {
            clearFilter();
            allowList = false;
        }
        warnedPlayer = null;
        if (previousSpecialty != specialty || generatorChanged || energyUpgradeChanged) capabilitiesChanged();
        sync();
        for (PylonUpgrade upgrade : returned) giveUpgrade(player, upgrade);
    }

    private boolean tryRemoveUpgrade(Player player) {
        if (!hasSuperUpgrade() && !breakTool.isEmpty()) {
            if (level instanceof ServerLevel) {
                giveBackBreakTool(player);
                sync();
            }
            return true;
        }
        PylonUpgrade upgrade = getInstalledUpgrades().stream().findFirst().orElse(null);
        if (upgrade == null) return false;
        if (!(level instanceof ServerLevel)) return true;
        if (upgrade == PylonUpgrade.ENCHANTING && !shelves.isEmpty()) {
            ItemStack shelf = shelves.remove(0);
            player.getInventory().placeItemBackInInventory(shelf);
            sync();
            return true;
        }

        if (upgrade == PylonUpgrade.FLUID_STORAGE && getFluidAmount() > 0) {
            warn(player, "tank_needs_empty");
            return true;
        }
        if ((upgrade == PylonUpgrade.XP_TANK && hasExperience()) ||
            (upgrade.isEnergy() && getEnergy() > 0) ||
            ((upgrade == PylonUpgrade.HARVESTER || upgrade.slot() == PylonUpgrade.Slot.PROCESSING) && !outputBuffer.isEmpty())) {
            warn(player, "specialty_needs_empty");
            return true;
        }
        if (upgrade.slot() == PylonUpgrade.Slot.STORAGE &&
            (getStoredCount() > PylonContents.CAPACITY || getFluidAmount() > BASE_FLUID_CAPACITY_MB ||
             xpFluidAmount() > PylonXpFluids.millibuckets(BASE_XP_CAPACITY))) {
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
        trimWorkshopLinks();
        updateChunkLoading();
        if (upgrade == PylonUpgrade.ANVIL || upgrade == PylonUpgrade.ENCHANTING) clearWorkshopLinks();
        if (upgrade == PylonUpgrade.FILTER) {
            clearFilter();
            allowList = false;
        }
        warnedPlayer = null;
        if (upgrade.isTank() || upgrade.isEnergy() || upgrade == PylonUpgrade.XP_TANK || upgrade == PylonUpgrade.VOID) capabilitiesChanged();
        sync();
        giveUpgrade(player, upgrade);
        return true;
    }

    private void giveBackBreakTool(Player player) {
        if (breakTool.isEmpty()) return;
        ItemStack removed = breakTool;
        breakTool = ItemStack.EMPTY;
        player.getInventory().placeItemBackInInventory(removed);
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
        updatePaintedState();
        updateChunkLoading();
        if (isWorkshop()) refreshWorkshopLinks();
        tickTransfers(server);
        PylonUpgrade specialty = getSpecialty();
        PylonUpgrade processor = getProcessing();
        if (isEnergy() && !isVoid()) {
            transferEnergy(server);
            if (energyUpdatePending && Math.floorMod(server.getGameTime(), 10) == Math.floorMod(worldPosition.hashCode(), 10)) {
                energyUpdatePending = false;
                sync();
            }
        }
        PylonUpgrade generator = getGenerator();
        if (specialty == null && processor == null && generator == null) return;
        if (specialty == PylonUpgrade.ENCHANTING) emitEnchantingParticles(server);
        PylonUpgrade speed = upgrades.get(PylonUpgrade.Slot.SPEED);
        int speedTier = speed == null ? 0 : speed.tier();
        if (specialty == PylonUpgrade.XP_TANK) transferExperience(server, 1 << speedTier);
        int interval = speed == null ? 60 : speed.value();
        if (Math.floorMod(server.getGameTime(), interval) != Math.floorMod(worldPosition.hashCode(), interval)) return;
        PylonUpgrade quantity = upgrades.get(PylonUpgrade.Slot.QUANTITY);
        int amount = quantity == null ? 1 : quantity.value();
        if (generator != null && !isVoid()) {
            if (generator.generatesFluid()) generateFluid(generator.generatedFluid(), amount * 100);
            else if (contents.add(new ItemStack(generator.generatedItem()), amount, getItemCapacity()) > 0) sync();
        }
        if (isWorkshop()) operateWorkshop(server);
        else if (specialty != null) {
            switch (specialty) {
                case FERTILIZER -> fertilize(server, Math.max(1, speedTier), amount);
                case HARVESTER -> harvest(server);
                case CLICKER -> clickTarget(server);
                case BREAK -> breakTarget(server);
                case MAGNET -> collectItems(server, amount);
                case PLANTER -> plant(server, amount);
                case PLACER -> place(server, workTarget(), workDirection.getOpposite());
                case DROPPER -> dropItems(server, amount);
                default -> { }
            }
        }
        if (processor != null) process(server, processor, amount, quantity == null ? 0 : quantity.tier());
    }

    private void dropItems(ServerLevel server, int amount) {
        if (contents.count() == 0) return;
        BlockPos target = worldPosition.relative(workDirection, dropDistance());
        if (!server.hasChunkAt(target) || !server.getWorldBorder().isWithinBounds(target)) return;
        Vec3 center = Vec3.atCenterOf(target);
        int remaining = amount;
        while (remaining > 0) {
            ItemStack stack = contents.peek(remaining);
            if (stack.isEmpty()) break;
            int count = stack.getCount();
            ItemEntity drop = new ItemEntity(server, center.x, center.y, center.z, stack);
            drop.setDeltaMovement(Vec3.ZERO);
            drop.setDefaultPickUpDelay();
            if (!server.noCollision(drop) || !server.addFreshEntity(drop)) break;
            contents.remove(count);
            remaining -= count;
        }
        if (remaining != amount) sync();
    }

    private void tickTransfers(ServerLevel server) {
        if (wires.isEmpty() && upgrades.values().stream().noneMatch(upgrade -> upgrade.transferType() != null)) return;
        long[] budget = { getEnergyCapacity() / 10L, Math.max(1, getItemCapacity() / 10), getFluidCapacity() / 10,
            getExperienceCapacity() / 10 };
        if (upgrades.get(PylonUpgrade.Slot.STORAGE) == PylonUpgrade.STORAGE_6) {
            Arrays.fill(budget, Long.MAX_VALUE);
        }
        boolean removed = false;
        for (Iterator<PylonWire> it = wires.iterator(); it.hasNext();) {
            PylonWire wire = it.next();
            if (!server.hasChunkAt(wire.target())) continue;
            if (!(server.getBlockEntity(wire.target()) instanceof PylonBlockEntityBase target) ||
                !wire.targetId().equals(target.wireId)) {
                it.remove();
                removed = true;
                continue;
            }
            int slot = wire.type().ordinal();
            if (budget[slot] <= 0 || !supportsTransfer(wire.type(), false) ||
                !target.supportsTransfer(wire.type(), true)) continue;
            budget[slot] -= moveToPylon(server, wire.type(), target, budget[slot]);
        }
        if (removed) sync();
        BlockPos adjacent = workTarget();
        if (!server.hasChunkAt(adjacent)) return;
        var neighbor = server.getBlockEntity(adjacent);
        for (PylonUpgrade upgrade : upgrades.values()) {
            PylonWire.Type type = upgrade.transferType();
            boolean importing = upgrade.importsResource();
            if (type == null || !supportsTransfer(type, importing) || budget[type.ordinal()] == 0) continue;
            long limit = isVoid() && importing ? Long.MAX_VALUE : budget[type.ordinal()];
            if (neighbor instanceof PylonBlockEntityBase pylon && pylon.supportsTransfer(type, !importing)) {
                if (importing) pylon.moveToPylon(server, type, this, limit);
                else moveToPylon(server, type, pylon, limit);
            } else if (type == PylonWire.Type.XP) {
                var storage = experienceAt(server, adjacent);
                if (storage != null && storage.getExperienceCapacity() > 0) {
                    if (importing) moveExperience(storage, this, limit);
                    else moveExperience(this, storage, limit);
                } else {
                    transferAdjacentExternal(server, adjacent, type, importing, limit);
                }
            } else {
                transferAdjacentExternal(server, adjacent, type, importing, limit);
            }
        }
    }

    private long moveToPylon(ServerLevel server, PylonWire.Type type, PylonBlockEntityBase target, long limit) {
        long moved = switch (type) {
            case ENERGY -> moveWireEnergy(server, target, (int) Math.min(Integer.MAX_VALUE, limit));
            case ITEM -> moveItemsTo(target, limit);
            case FLUID -> transferFluidTo(target, limit);
            case XP -> {
                long points = Math.min(limit, Math.min(experience, target.wholeExperienceSpace()));
                experience -= points;
                if (!target.isVoid()) target.experience += points;
                yield points;
            }
        };
        if (moved > 0 && type != PylonWire.Type.ENERGY) {
            sync();
            if (!target.isVoid()) target.sync();
        }
        return moved;
    }

    private long moveItemsTo(PylonBlockEntityBase target, long limit) {
        if (!target.canStoreItem(getStoredItem())) return 0;
        if (!target.isVoid()) return contents.moveTo(target.contents, limit, target.getItemCapacity());
        long removed = Math.min(limit, getStoredCount());
        contents.remove(removed);
        return removed;
    }

    private static long moveExperience(PylonExperienceStorage source, PylonExperienceStorage target, long limit) {
        long offered = source.extractExperience(limit, true);
        long accepted = target.receiveExperience(offered, true);
        long extracted = source.extractExperience(accepted, false);
        long inserted = target.receiveExperience(extracted, false);
        if (inserted < extracted) {
            long remainder = extracted - inserted;
            if (source instanceof PylonBlockEntityBase pylon) {
                pylon.experience += remainder;
                pylon.sync();
            } else {
                source.receiveExperience(remainder, false);
            }
        }
        return inserted;
    }

    protected boolean allowsAutomaticEnergyOutput() {
        return !upgrades.containsKey(PylonUpgrade.Slot.ENERGY_TRANSFER);
    }

    protected boolean allowsAutomaticEnergyInput(ServerLevel server, BlockPos above) {
        return !server.hasChunkAt(above) || !(server.getBlockEntity(above) instanceof PylonBlockEntityBase source) ||
            source.allowsAutomaticEnergyOutput();
    }

    private int moveWireEnergy(ServerLevel server, PylonBlockEntityBase target, int limit) {
        int amount = target.energyLimit(true, energyLimit(false, limit));
        if (amount <= 0) return 0;
        long now = server.getGameTime();
        energy.transfer(false, amount, now);
        if (!target.isVoid()) {
            target.energy.transfer(true, amount, now);
            target.energyChanged();
        }
        energyChanged();
        return amount;
    }

    private void collectItems(ServerLevel server, int maximum) {
        if (getStoredCount() >= getItemCapacity()) return;
        int radius = range();
        AABB area = new AABB(worldPosition).inflate(radius);
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

    private void process(ServerLevel server, PylonUpgrade processor, int amount, int tier) {
        var output = workOutput(server);
        if (output == null) return;
        flushOutput(output);
        if (!outputBuffer.isEmpty() || getStoredCount() == 0) return;
        ItemStack input = getStoredItem();
        List<PylonPulverizing.Result> recipe;
        if (processor == PylonUpgrade.SMELT) {
            ItemStack smelted = smeltingRecipe(server, input);
            if (smelted.isEmpty()) return;
            recipe = List.of(new PylonPulverizing.Result(smelted, 100));
        } else {
            recipe = PylonPulverizing.find(server, input, tier);
            if (recipe.isEmpty()) return;
        }
        long previousFuel = smeltFuel;
        boolean processed = false;
        ItemStack first = recipe.get(0).stack();
        for (int i = 0; i < amount && outputBuffer.isEmpty() && PylonItemRules.exactlyEqual(input, getStoredItem()); i++) {
            if (!output.apply(first.copyWithCount(Math.min(first.getCount(), first.getMaxStackSize())), true).isEmpty() ||
                !consumeProcessingFuel(server)) break;
            if (isRemoved() || !PylonItemRules.exactlyEqual(input, getStoredItem())) break;
            contents.extract(1);
            for (PylonPulverizing.Result result : recipe) {
                if (result.chance() < 100 && server.getRandom().nextInt(100) >= result.chance()) continue;
                ItemStack stack = result.stack();
                for (int count = stack.getCount(); count > 0; count -= stack.getMaxStackSize()) {
                    outputBuffer.add(stack.copyWithCount(Math.min(count, stack.getMaxStackSize())));
                }
            }
            flushOutput(output);
            processed = true;
        }
        if (!isRemoved() && (processed || smeltFuel != previousFuel)) sync();
    }

    private boolean consumeProcessingFuel(ServerLevel server) {
        if (smeltFuel < FURNACE_TICKS_PER_ITEM) {
            if (consumeSmeltingEnergy(server)) return true;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos source = worldPosition.relative(direction);
                if (!server.hasChunkAt(source)) continue;
                int taken = takeSmeltingFuel(server, source, direction.getOpposite());
                if (taken <= 0) continue;
                smeltFuel += taken;
                break;
            }
        }
        if (smeltFuel < FURNACE_TICKS_PER_ITEM) return false;
        smeltFuel -= FURNACE_TICKS_PER_ITEM;
        return true;
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
        int reach = Math.max(2, range());
        for (ExperienceOrb orb : server.getEntitiesOfClass(ExperienceOrb.class, block.inflate(reach),
                orb -> orb.isAlive() && block.distanceToSqr(orb.position()) <= reach * reach)) {
            long points = orbExperience(orb);
            if (points > 0 && points <= wholeExperienceSpace()) {
                experience += points;
                orb.discard();
            }
        }
        for (ServerPlayer player : playersInArea(server, new AABB(worldPosition.relative(workDirection.getOpposite())))) {
            experience += PylonExperience.collect(player, levels, wholeExperienceSpace());
        }
        if (experience > 0) {
            AABB outputArea = new AABB(workTarget()).minmax(new AABB(worldPosition.relative(workDirection, 2)));
            for (ServerPlayer player : playersInArea(server, outputArea)) {
                experience -= PylonExperience.dispense(player, levels, experience);
            }
        }
        if (experience != before) sync();
    }

    private static List<ServerPlayer> playersInArea(ServerLevel server, AABB area) {
        return server.getEntitiesOfClass(ServerPlayer.class, area,
            player -> player.isAlive() && !player.isSpectator() && area.contains(player.position()));
    }

    private int range() {
        PylonUpgrade range = upgrades.get(PylonUpgrade.Slot.RANGE);
        return range == null ? 1 : range.value();
    }

    private Iterable<BlockPos> farmArea() {
        int radius = range();
        return BlockPos.betweenClosed(worldPosition.offset(-radius, -radius, -radius),
            worldPosition.offset(radius, radius, radius));
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
        BiFunction<ItemStack, Boolean, ItemStack> output = workOutput(server);
        if (output != null) flushOutput(output);
        boolean harvestCrops = outputBuffer.isEmpty();
        ServerPlayer automation = getAutomationPlayer(server, AUTOMATION_PROFILE);
        for (BlockPos pos : farmArea()) {
            if (!server.hasChunkAt(pos)) continue;
            BlockState state = server.getBlockState(pos);
            if (state.is(BlockTags.LOGS) && harvestTree(server, automation, pos)) return;
            if (!harvestCrops) continue;
            PylonFarming.Harvest harvest = PylonFarming.harvest(server, pos, state);
            if (harvest == null || !mayWorkAt(server, automation, pos)) continue;
            if (output != null && harvest.drops().stream()
                .noneMatch(drop -> output.apply(drop.copy(), true).getCount() < drop.getCount())) continue;
            if (harvest.drops().isEmpty() || !server.setBlockAndUpdate(pos, harvest.remainder())) continue;
            if (output == null) {
                for (ItemStack drop : harvest.drops()) {
                    if (allowsItem(drop)) contents.insert(drop, getItemCapacity());
                    if (!drop.isEmpty()) Block.popResource(server, pos, drop);
                }
                sync();
            } else {
                outputBuffer.addAll(harvest.drops());
                setChanged();
                flushOutput(output);
            }
            return;
        }
    }

    private boolean harvestTree(ServerLevel server, ServerPlayer automation, BlockPos root) {
        var tree = PylonFarming.tree(server, root);
        if (tree.isEmpty() || tree.stream().anyMatch(pos -> !mayWorkAt(server, automation, pos))) return false;
        boolean harvested = false;
        for (var pos : tree) {
            var drops = PylonBreaker.breakBlock(server, pos, automation, new ItemStack(Items.DIAMOND_AXE)).drops();
            for (ItemEntity drop : drops) {
                ItemStack stack = drop.getItem().copy();
                if (allowsItem(stack) && contents.insert(stack, getItemCapacity()) > 0) {
                    harvested = true;
                    if (stack.isEmpty()) drop.discard();
                    else drop.setItem(stack);
                }
            }
        }
        if (harvested) sync();
        return true;
    }

    private void plant(ServerLevel server, int maximum) {
        if (!PylonFarming.canPlant(getStoredItem())) return;
        for (BlockPos pos : farmArea()) {
            if (getStoredCount() == 0 || maximum == 0) return;
            if (place(server, pos, Direction.UP)) maximum--;
        }
    }

    private boolean place(ServerLevel server, BlockPos pos, Direction face) {
        ItemStack stack = contents.peek(1);
        if (!(stack.getItem() instanceof BlockItem block) || !server.hasChunkAt(pos) ||
            !server.getBlockState(pos).isAir() || !block.getBlock().defaultBlockState().canSurvive(server, pos)) return false;
        ServerPlayer automation = getAutomationPlayer(server, AUTOMATION_PROFILE);
        if (!mayWorkAt(server, automation, pos)) return false;
        Vec3 previousPosition = automation.position();
        ItemStack previousItem = automation.getMainHandItem();
        Vec3 hit = Vec3.atCenterOf(pos);
        automation.setPos(hit.x, hit.y, hit.z);
        automation.setItemInHand(InteractionHand.MAIN_HAND, stack);
        try {
            stack.useOn(new UseOnContext(automation, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, pos, false)));
            if (stack.isEmpty() && !isRemoved()) {
                contents.remove(1);
                sync();
                return true;
            }
            return false;
        } finally {
            automation.setItemInHand(InteractionHand.MAIN_HAND, previousItem);
            automation.setPos(previousPosition.x, previousPosition.y, previousPosition.z);
        }
    }

    private BiFunction<ItemStack, Boolean, ItemStack> workOutput(ServerLevel server) {
        BlockPos target = workTarget();
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

    private void breakTarget(ServerLevel server) {
        long capacity = getItemCapacity();
        BlockPos target = workTarget();
        if (contents.count() >= capacity || !server.hasChunkAt(target)) return;
        BlockState state = server.getBlockState(target);
        if (state.isAir() || state.getBlock() instanceof LiquidBlock || state.getDestroySpeed(server, target) < 0) return;
        ServerPlayer automation = getAutomationPlayer(server, AUTOMATION_PROFILE);
        if (!mayWorkAt(server, automation, target)) return;
        ItemStack installed = breakTool;
        ItemStack used = installed.isEmpty() ? new ItemStack(Items.DIAMOND_PICKAXE) : installed.copy();
        var result = PylonBreaker.breakBlock(server, target, automation, used);
        if (isRemoved()) return;
        boolean toolChanged = !installed.isEmpty() && breakTool == installed &&
            !PylonItemRules.exactlyEqual(installed, result.tool());
        if (toolChanged) {
            if (!result.tool().isEmpty() && !PylonItemRules.isMiningTool(result.tool())) {
                Block.popResource(server, worldPosition, result.tool());
                breakTool = ItemStack.EMPTY;
            } else {
                breakTool = result.tool().copy();
            }
        }
        if (collectBreakDrops(result.drops(), capacity) || toolChanged) sync();
    }

    private boolean collectBreakDrops(List<ItemEntity> drops, long capacity) {
        if (drops.isEmpty()) return false;
        ItemStack first = drops.get(0).getItem().copyWithCount(1);
        if (!canStoreItem(first)) return false;
        long previousCount = contents.count();
        for (ItemEntity drop : drops) {
            ItemStack stack = drop.getItem().copy();
            if (!PylonItemRules.exactlyEqual(first, stack)) continue;
            if (contents.insert(stack, capacity) == 0) break;
            if (stack.isEmpty()) drop.discard();
            else drop.setItem(stack);
        }
        return contents.count() != previousCount;
    }

    private void clickTarget(ServerLevel server) {
        BlockPos target = workTarget();
        if (!server.hasChunkAt(target) || server.getBlockState(target).isAir() ||
            !server.getWorldBorder().isWithinBounds(target)) return;
        ServerPlayer clicker = getAutomationPlayer(server, AUTOMATION_PROFILE);
        if (!mayWorkAt(server, clicker, target)) return;

        Direction face = workDirection.getOpposite();
        Vec3 hit = Vec3.atCenterOf(target).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        clicker.setPos(hit.x, hit.y, hit.z);
        clicker.setXRot(face.getStepY() * 90);
        clicker.setYRot(face.getAxis().isHorizontal() ? workDirection.toYRot() : 0);
        ItemStack original = contents.displayItem();
        clicker.setItemInHand(InteractionHand.MAIN_HAND, contents.extract(1));
        try {
            clicker.gameMode.useItemOn(clicker, server, clicker.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(hit, face, target, false));
        } finally {
            ItemStack remaining = clicker.getMainHandItem();
            boolean changed = remaining.getCount() != original.getCount() || !PylonItemRules.exactlyEqual(original, remaining);
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
        return packedDrop || hasExperience() || energy.amount() > 0 || smeltFuel > 0 || (getStoredCount() > 0 &&
            getStoredCount() > (long) MAX_LOOSE_STACKS * getStoredItem().getMaxStackSize());
    }

    public void dropContents() {
        if (!(level instanceof ServerLevel server) || contentsDropped) return;
        contentsDropped = true;
        updateChunkLoading(server, 0);
        clearFluid();
        wires.clear();
        clearWorkshopLinks();
        for (ItemStack output : outputBuffer) Block.popResource(level, worldPosition, output);
        outputBuffer.clear();
        packedDrop = hasPackedDrop();
        if (packedDrop) {
            Block.popResource(level, worldPosition, createPackedDrop());
            contents.restore(ItemStack.EMPTY, 0);
        } else {
            Block.popResource(level, worldPosition, breakTool);
            while (contents.count() > 0) {
                Block.popResource(level, worldPosition, contents.extract(Integer.MAX_VALUE));
            }
            for (PylonUpgrade upgrade : upgrades.values()) {
                Block.popResource(level, worldPosition, upgradeStack(upgrade));
            }
            for (ItemStack shelf : shelves) Block.popResource(level, worldPosition, shelf);
        }
        breakTool = ItemStack.EMPTY;
        shelves.clear();
        upgrades.clear();
    }

    protected void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
}
