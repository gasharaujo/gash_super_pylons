package net.gasharaujo.superpylons.block;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
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
import net.minecraft.world.entity.Mob;
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
    private static final int EXCAVATION_SCAN_BUDGET = 4096;
    static final int FILTER_SIZE = 27;

    protected final PylonContents contents = new PylonContents(this::isCreativeStorage, this::getItemCapacity);
    protected final PylonEnergy energy = new PylonEnergy(this::isCreativeStorage);
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
    private final List<PylonWire> wireInputs = new ArrayList<>();
    private ItemStack breakTool = ItemStack.EMPTY;
    private ItemStack furnaceAttachment = ItemStack.EMPTY;
    private int excavationCursor;
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

    public boolean isCreativeStorage() { return upgrades.containsKey(PylonUpgrade.Slot.CREATIVE); }
    public boolean hasBulkTransfer() { return upgrades.get(PylonUpgrade.Slot.STORAGE) == PylonUpgrade.STORAGE_6; }
    public long getAvailableItemCount() { return contents.available(); }
    public int getAvailableEnergy() { return energy.available(getEnergyCapacity()); }
    public long getAvailableExperience() {
        return isCreativeStorage() && hasExperience() ? Math.max(experience, getExperienceCapacity()) : experience;
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
    public String getWorkshopStatus() { return contents.isWorkshopOutput() ? "complete" : workshopStatus; }
    public long getWorkshopCost() {
        return contents.isWorkshopOutput() || workshopStatus.equals("xp") ? workshopCost : 0;
    }
    public int getEnchantmentPower() {
        PylonUpgrade capacity = upgrades.get(PylonUpgrade.Slot.STORAGE);
        return capacity == null ? 1 : capacity.tier() == 6 ? 100 : capacity.tier() * 10;
    }
    public int getMinimumEnchantments() {
        PylonUpgrade quantity = upgrades.get(PylonUpgrade.Slot.QUANTITY);
        return quantity == null ? 1 : quantity.tier() + 1;
    }

    protected void loadWorkshopStatus(String status, long cost) {
        if (status.equals("complete") && isWorkshop()) {
            contents.markWorkshopOutput();
            status = "input";
        }
        workshopStatus = List.of("connections", "input", "lapis", "second_item", "no_result", "xp").contains(status)
            ? status : "connections";
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
    public ItemStack getFurnaceAttachment() { return furnaceAttachment.copy(); }

    protected void loadFurnaceAttachment(ItemStack attachment) {
        furnaceAttachment = getProcessing() == PylonUpgrade.SMELT && isFurnaceAttachment(attachment) ?
            attachment.copyWithCount(1) : ItemStack.EMPTY;
    }

    protected int excavationCursor() { return excavationCursor; }

    protected void loadExcavationCursor(int cursor) { excavationCursor = Math.max(0, cursor); }

    private static boolean isFurnaceAttachment(ItemStack stack) {
        return stack.is(Items.BLAST_FURNACE) || stack.is(Items.SMOKER);
    }

    protected void loadBreakTool(ItemStack tool) {
        boolean valid = getSpecialty() == PylonUpgrade.BREAK && PylonItemRules.isMiningTool(tool) ||
            getSpecialty() == PylonUpgrade.HARVESTER && PylonItemRules.isWeapon(tool);
        breakTool = valid ? tool.copyWithCount(1) : ItemStack.EMPTY;
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
        if (contents.isWorkshopOutput()) {
            if (exportWorkshopOutput(server) && !isRemoved()) sync();
            return;
        }
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
        if (xp.getAvailableExperience() < cost) { workshopStatus("xp", cost); return; }
        contents.restore(result.item(), 1, getItemCapacity());
        contents.markWorkshopOutput();
        if (!xp.isCreativeStorage()) xp.experience -= cost;
        xp.sync();
        if (enchanting || !hasSuperUpgrade() || !material.getStoredItem().is(Items.ENCHANTED_BOOK)) {
            material.contents.remove(1);
            material.sync();
        }
        workshopStatus = "input";
        workshopCost = cost;
        exportWorkshopOutput(server);
        if (!isRemoved()) sync();
    }

    private boolean exportWorkshopOutput(ServerLevel server) {
        BlockPos target = workTarget();
        for (Auxiliary link : auxiliaries) {
            if (link.pos().equals(target) && auxiliary(link) != null) return false;
        }
        var output = workOutput(server);
        if (output == null || !output.apply(contents.peek(1), true).isEmpty() ||
            !contents.isWorkshopOutput() || isRemoved()) return false;
        boolean copied = isCreativeStorage();
        ItemStack remainder = output.apply(contents.extract(1), false);
        if (remainder.isEmpty()) return true;
        if (!copied) {
            if (!isRemoved() && getStoredCount() == 0) {
                contents.restore(remainder, 1, getItemCapacity());
                if (isWorkshop()) contents.markWorkshopOutput();
            } else {
                Block.popResource(server, worldPosition, remainder);
                return true;
            }
        }
        return false;
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

    protected String saveWireInputs() {
        return wireInputs.stream().map(PylonWire::encode).collect(Collectors.joining(","));
    }

    protected void loadWireInputs(String saved) {
        wireInputs.clear();
        for (String entry : saved.split(",")) {
            PylonWire wire = PylonWire.decode(entry);
            if (wire != null && validWireDistance(wire.target()) && !wireInputs.contains(wire)) wireInputs.add(wire);
        }
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
        target.recordWireInput(type, this);
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
                target.wireInputs.remove(new PylonWire(wire.type(), worldPosition, wireId));
                target.sync();
                player.getInventory().placeItemBackInInventory(wire.type().itemStack());
                removed = true;
            }
        }
        if (removed) sync();
        return removed;
    }

    private void recordWireInput(PylonWire.Type type, PylonBlockEntityBase source) {
        PylonWire input = new PylonWire(type, source.worldPosition, source.wireId);
        if (!wireInputs.contains(input)) {
            wireInputs.add(input);
            sync();
        }
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

    protected long availableXpFluid() {
        return isCreativeStorage() && hasExperience() ? Math.max(xpFluidAmount(), xpFluidCapacity()) : xpFluidAmount();
    }

    protected int xpFluidLimit(boolean input, Fluid fluid, int requested) {
        if (!(level instanceof ServerLevel) || isRemoved() || !isXpTank() || requested <= 0 || !PylonXpFluids.supports(fluid)) return 0;
        long available = input ? Math.max(0, PylonXpFluids.millibuckets(experienceSpace()) - experienceRemainder) : availableXpFluid();
        return (int) Math.min(requested, available);
    }

    protected void moveXpFluid(boolean input, Fluid fluid, long amount) {
        int ratio = PylonXpFluids.MILLIBUCKETS_PER_POINT;
        if (input) {
            if (!hasExperience() || experienceFluid.isEmpty()) experienceFluid = PylonXpFluids.id(fluid);
            int remainder = experienceRemainder + (int) (amount % ratio);
            experience += amount / ratio + remainder / ratio;
            experienceRemainder = remainder % ratio;
        } else if (!isCreativeStorage()) {
            int remainder = experienceRemainder - (int) (amount % ratio);
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
        long taken = Math.min(points, getAvailableExperience());
        if (!simulate && taken > 0 && !isCreativeStorage()) {
            experience -= taken;
            sync();
        }
        return taken;
    }

    public int comparatorSignal() {
        long amount = getAvailableItemCount();
        long capacity = getItemCapacity();
        if (isEnergy()) {
            amount = getAvailableEnergy();
            capacity = getEnergyCapacity();
        } else if (isTank()) {
            amount = isCreativeStorage() && getFluidAmount() > 0 ? getFluidCapacity() : getFluidAmount();
            capacity = getFluidCapacity();
        } else if (getSpecialty() == PylonUpgrade.XP_TANK) {
            amount = availableXpFluid();
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
        return energy.limit(input, requested, getEnergyCapacity(), hasBulkTransfer() ? Integer.MAX_VALUE : upgrades.get(PylonUpgrade.Slot.ENERGY).energyTransfer(),
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
    protected interface ProcessingSource {
        int slots();
        ItemStack extractOne(int slot, boolean simulate);
        int stackSize(int slot);
    }

    protected abstract ProcessingSource processingSource(ServerLevel server, BlockPos pos, Direction side);
    protected abstract ItemStack smeltingRecipe(ServerLevel server, ItemStack input);
    protected abstract int minimumY(ServerLevel server);
    protected abstract boolean collectFluidSource(ServerLevel server, ServerPlayer player, BlockPos pos, BlockState state);
    protected abstract List<ItemStack> dispense(ServerLevel server, BlockPos origin, Direction direction, ItemStack stack);
    protected abstract void attackMob(ServerPlayer player, Mob mob, ItemStack weapon);
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
        if (hasBulkTransfer()) return transferItemBatch(amount, simulate);
        if (simulate) return contents.peek(amount);
        ItemStack extracted = contents.extract(amount);
        if (!extracted.isEmpty()) sync();
        return extracted;
    }

    protected ItemStack transferItemBatch(int maximum, boolean simulate) {
        if (!canExtractItems() || maximum <= 0 || getAvailableItemCount() == 0) return ItemStack.EMPTY;
        ItemStack batch = getStoredItem().copyWithCount((int) Math.min(maximum, getAvailableItemCount()));
        if (!simulate) {
            contents.remove(batch.getCount());
            sync();
        }
        return batch;
    }

    protected void returnTransferItems(ItemStack remainder, boolean copied) {
        if (remainder.isEmpty() || copied) return;
        if (!isRemoved() && !contentsDropped) {
            int restored = contents.add(remainder, remainder.getCount(), getItemCapacity());
            remainder = remainder.copyWithCount(remainder.getCount() - restored);
            if (restored > 0) sync();
        }
        while (!remainder.isEmpty()) {
            ItemStack drop = remainder.split(remainder.getMaxStackSize());
            Block.popResource(level, worldPosition, drop);
        }
    }

    protected boolean hasItemStorage() {
        PylonUpgrade specialty = getSpecialty();
        return !isRemoved() && (specialty == null || !specialty.locksItems());
    }

    protected boolean canExtractItems() {
        return !isRemoved() && !isVoid() && (hasItemStorage() || getStoredCount() > 0);
    }

    protected boolean canStoreItem(ItemStack input) {
        if (isRemoved() || input.isEmpty() || input.is(Items.COMMAND_BLOCK) || input.getItem() instanceof PylonUpgradeItem || PylonItemRules.isModifier(input) ||
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
        if (held.is(Items.COMMAND_BLOCK)) {
            if (level instanceof ServerLevel) install(player, held, PylonUpgrade.CREATIVE_STORAGE);
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
        if (getProcessing() == PylonUpgrade.SMELT && isFurnaceAttachment(held)) {
            if (level instanceof ServerLevel) {
                ItemStack previous = furnaceAttachment;
                furnaceAttachment = held.copyWithCount(1);
                held.shrink(1);
                if (!previous.isEmpty()) player.getInventory().placeItemBackInInventory(previous);
                sync();
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
        } else if (held.is(Items.GLASS_BOTTLE) && getAvailableExperience() >= XP_PER_BOTTLE) {
            if (!isCreativeStorage()) experience -= XP_PER_BOTTLE;
            returned = Items.EXPERIENCE_BOTTLE;
        } else {
            return;
        }
        held.shrink(1);
        player.getInventory().placeItemBackInInventory(new ItemStack(returned));
        sync();
    }

    public boolean tryInstallWorkTool(Player player, InteractionHand hand) {
        if (player.isSpectator() || hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) return false;
        ItemStack held = player.getMainHandItem();
        PylonUpgrade specialty = getSpecialty();
        boolean valid = specialty == PylonUpgrade.BREAK && PylonItemRules.isMiningTool(held) ||
            specialty == PylonUpgrade.HARVESTER && hasSuperUpgrade() && PylonItemRules.isWeapon(held);
        if (!valid) return false;
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
        if (next.slot() == PylonUpgrade.Slot.RANGE) excavationCursor = 0;
        updateChunkLoading();
        warnedPlayer = null;
        if (next.isTank() || next.isEnergy() || next == PylonUpgrade.XP_TANK || next == PylonUpgrade.VOID || next == PylonUpgrade.CREATIVE_STORAGE) capabilitiesChanged();
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
        boolean creativeChanged = isCreativeStorage() != next.containsKey(PylonUpgrade.Slot.CREATIVE);
        PylonUpgrade processor = next.get(PylonUpgrade.Slot.PROCESSING);
        boolean rangeChanged = upgrades.get(PylonUpgrade.Slot.RANGE) != next.get(PylonUpgrade.Slot.RANGE);
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
        if (previousSpecialty != specialty) giveBackBreakTool(player);
        if (processor != PylonUpgrade.SMELT) giveBackFurnaceAttachment(player);
        if (previousSpecialty != specialty || rangeChanged) excavationCursor = 0;
        if (previousSpecialty != specialty) {
            contents.clearWorkshopOutput();
            clearWorkshopLinks();
            for (ItemStack shelf : shelves) player.getInventory().placeItemBackInInventory(shelf);
            shelves.clear();
        }
        if (!hasFilter()) {
            clearFilter();
            allowList = false;
        }
        warnedPlayer = null;
        if (previousSpecialty != specialty || generatorChanged || energyUpgradeChanged || creativeChanged) capabilitiesChanged();
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
        if (upgrade == PylonUpgrade.SMELT && !furnaceAttachment.isEmpty()) {
            giveBackFurnaceAttachment(player);
            sync();
            return true;
        }
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
            ((upgrade == PylonUpgrade.HARVESTER || upgrade == PylonUpgrade.BREAK || upgrade.slot() == PylonUpgrade.Slot.PROCESSING) &&
                !outputBuffer.isEmpty())) {
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
        if (upgrade == PylonUpgrade.BREAK || upgrade == PylonUpgrade.FLUID_STORAGE ||
            upgrade.slot() == PylonUpgrade.Slot.RANGE) excavationCursor = 0;
        trimWorkshopLinks();
        updateChunkLoading();
        if (upgrade == PylonUpgrade.ANVIL || upgrade == PylonUpgrade.ENCHANTING) {
            contents.clearWorkshopOutput();
            clearWorkshopLinks();
        }
        if (upgrade == PylonUpgrade.FILTER) {
            clearFilter();
            allowList = false;
        }
        warnedPlayer = null;
        if (upgrade.isTank() || upgrade.isEnergy() || upgrade == PylonUpgrade.XP_TANK || upgrade == PylonUpgrade.VOID || upgrade == PylonUpgrade.CREATIVE_STORAGE) capabilitiesChanged();
        sync();
        giveUpgrade(player, upgrade);
        return true;
    }

    private void giveBackFurnaceAttachment(Player player) {
        if (furnaceAttachment.isEmpty()) return;
        ItemStack removed = furnaceAttachment;
        furnaceAttachment = ItemStack.EMPTY;
        player.getInventory().placeItemBackInInventory(removed);
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
                case HARVESTER -> {
                    if (hasSuperUpgrade()) attackMobs(server, quantity == null ? 1 : quantity.tier() + 1);
                    else harvest(server);
                }
                case CLICKER -> clickTarget(server);
                case BREAK -> {
                    if (hasSuperUpgrade()) excavate(server, amount, false);
                    else breakTarget(server);
                }
                case FLUID_STORAGE -> {
                    if (hasSuperUpgrade()) pumpFluids(server, amount);
                }
                case MAGNET -> collectItems(server, amount);
                case PLANTER -> plant(server, amount);
                case PLACER -> {
                    if (hasSuperUpgrade()) fillPlane(server, amount);
                    else place(server, workTarget(), workDirection.getOpposite());
                }
                case DROPPER -> {
                    if (hasSuperUpgrade()) dispenseItems(server, amount);
                    else dropItems(server, amount);
                }
                default -> { }
            }
        }
        if (processor != null) process(server, processor, amount, quantity == null ? 0 : quantity.tier());
    }

    private void dispenseItems(ServerLevel server, int amount) {
        BlockPos mouth = worldPosition.relative(workDirection, dropDistance() - 1);
        BlockPos target = mouth.relative(workDirection);
        if (!server.hasChunkAt(mouth) || !server.hasChunkAt(target)) return;
        ServerPlayer automation = getAutomationPlayer(server, AUTOMATION_PROFILE);
        if (!mayWorkAt(server, automation, target)) return;
        for (BlockPos pos = workTarget(); !pos.equals(target); pos = pos.relative(workDirection)) {
            if (!server.hasChunkAt(pos) || !server.getBlockState(pos).getCollisionShape(server, pos).isEmpty()) return;
        }
        boolean changed = false;
        boolean creative = isCreativeStorage();
        for (int i = 0; i < amount && !isRemoved(); i++) {
            ItemStack stack = contents.extract(1);
            if (stack.isEmpty()) break;
            ItemStack original = stack.copy();
            var returned = dispense(server, mouth, workDirection, stack);
            boolean used = returned.size() != 1 || returned.get(0).getCount() != 1 ||
                !PylonItemRules.exactlyEqual(original, returned.get(0));
            for (ItemStack remainder : returned) {
                if (creative && PylonItemRules.exactlyEqual(original, remainder)) continue;
                int kept = isRemoved() ? 0 : contents.add(remainder, remainder.getCount(), getItemCapacity());
                remainder.shrink(kept);
                if (!remainder.isEmpty()) Block.popResource(server, worldPosition, remainder);
            }
            if (!used) break;
            changed = true;
        }
        if (changed && !isRemoved()) sync();
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
        PylonUpgrade capacity = upgrades.get(PylonUpgrade.Slot.STORAGE);
        int tier = capacity == null ? 0 : capacity.tier();
        long[] budget = {
            getEnergyCapacity() / 10L,
            PylonUpgrade.itemTransfer(tier, server.getGameTime(), worldPosition.hashCode()),
            PylonUpgrade.fluidTransfer(tier),
            getExperienceCapacity() / 10
        };
        if (hasBulkTransfer()) {
            Arrays.fill(budget, Long.MAX_VALUE);
        }
        boolean removed = false;
        List<ItemStack> detached = new ArrayList<>();
        for (Iterator<PylonWire> it = wires.iterator(); it.hasNext();) {
            PylonWire wire = it.next();
            if (!server.hasChunkAt(wire.target())) continue;
            if (!(server.getBlockEntity(wire.target()) instanceof PylonBlockEntityBase target) ||
                !wire.targetId().equals(target.wireId)) {
                it.remove();
                detached.add(wire.type().itemStack());
                removed = true;
                continue;
            }
            int slot = wire.type().ordinal();
            target.recordWireInput(wire.type(), this);
            boolean bulk = hasBulkTransfer() || target.hasBulkTransfer();
            if ((!bulk && budget[slot] <= 0) || !supportsTransfer(wire.type(), false) ||
                !target.supportsTransfer(wire.type(), true)) continue;
            long moved = moveToPylon(server, wire.type(), target, bulk ? Long.MAX_VALUE : budget[slot]);
            if (!bulk) budget[slot] -= moved;
        }
        if (removed) sync();
        for (ItemStack wire : detached) Block.popResource(server, worldPosition, wire);
        BlockPos adjacent = workTarget();
        if (!server.hasChunkAt(adjacent)) return;
        var neighbor = server.getBlockEntity(adjacent);
        for (PylonUpgrade upgrade : upgrades.values()) {
            PylonWire.Type type = upgrade.transferType();
            boolean importing = upgrade.importsResource();
            if (type == null || !supportsTransfer(type, importing)) continue;
            boolean bulk = hasBulkTransfer() || neighbor instanceof PylonBlockEntityBase pylon && pylon.hasBulkTransfer();
            if (!bulk && budget[type.ordinal()] == 0) continue;
            boolean unboundedVoid = isVoid() && importing && (type == PylonWire.Type.ENERGY || type == PylonWire.Type.XP);
            long limit = bulk || unboundedVoid ? Long.MAX_VALUE : budget[type.ordinal()];
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
                long points = Math.min(limit, Math.min(getAvailableExperience(), target.wholeExperienceSpace()));
                if (!isCreativeStorage()) experience -= points;
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
        long removed = Math.min(limit, getAvailableItemCount());
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
                if (!pylon.isCreativeStorage()) {
                    pylon.experience += remainder;
                    pylon.sync();
                }
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
        boolean bulk = hasBulkTransfer() || target.hasBulkTransfer();
        int amount = bulk ? Math.max(0, Math.min(limit, Math.min(getAvailableEnergy(),
            target.getEnergyCapacity() - target.getEnergy()))) : target.energyLimit(true, energyLimit(false, limit));
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
        boolean direct = hasSuperUpgrade();
        var output = direct ? workOutput(server) : null;
        if (direct ? output == null : getStoredCount() >= getItemCapacity()) return;
        AABB area = new AABB(worldPosition).inflate(range());
        for (ItemEntity entity : server.getEntitiesOfClass(ItemEntity.class, area,
                item -> item.isAlive() && area.contains(item.position()))) {
            ItemStack stack = entity.getItem();
            ItemStack sample = stack.copyWithCount(Math.min(maximum, stack.getCount()));
            int inserted;
            if (direct) {
                if (PylonItemRules.isModifier(sample) || !allowsItem(sample)) continue;
                int offered = sample.getCount();
                inserted = offered - output.apply(sample, false).getCount();
            } else {
                inserted = insertFrom(sample);
            }
            if (inserted == 0) continue;
            if (inserted == stack.getCount()) entity.discard();
            else entity.setItem(stack.copyWithCount(stack.getCount() - inserted));
            maximum -= inserted;
            if (maximum == 0 || !direct && getStoredCount() >= getItemCapacity()) return;
        }
    }

    private void process(ServerLevel server, PylonUpgrade processor, int amount, int tier) {
        var output = workOutput(server);
        if (output == null) return;
        flushOutput(output);
        if (!outputBuffer.isEmpty()) return;

        long previousFuel = smeltFuel;
        long previousCount = getStoredCount();
        BlockPos sourcePos = worldPosition.relative(workDirection.getOpposite());
        if (hasSuperUpgrade() && server.hasChunkAt(sourcePos)) {
            ProcessingSource source = processingSource(server, sourcePos, workDirection);
            if (source != null) {
                for (int slot = 0; slot < source.slots() && outputBuffer.isEmpty() && !isRemoved(); slot++) {
                    processSlot(server, processor, tier, output, source, slot, sourcePos);
                }
            }
        }

        ItemStack input = getStoredItem();
        if (!input.isEmpty() && !contents.isWorkshopOutput() && outputBuffer.isEmpty() && !isRemoved()) {
            var recipe = processingRecipe(server, processor, input, tier);
            for (int i = 0; i < amount && !recipe.isEmpty() && outputBuffer.isEmpty() && !isRemoved(); i++) {
                if (!processOne(server, output, recipe, hasSuperUpgrade() ? sourcePos : null, () ->
                    PylonItemRules.exactlyEqual(input, getStoredItem()) && !contents.extract(1).isEmpty())) break;
            }
        }
        if (!isRemoved() && (previousFuel != smeltFuel || previousCount != getStoredCount() ||
            !outputBuffer.isEmpty())) sync();
    }

    private void processSlot(ServerLevel server, PylonUpgrade processor, int tier,
                             BiFunction<ItemStack, Boolean, ItemStack> output, ProcessingSource source,
                             int slot, BlockPos sourcePos) {
        ItemStack input = source.extractOne(slot, true);
        if (input.isEmpty() || !allowsItem(input)) return;
        var recipe = processingRecipe(server, processor, input, tier);
        if (recipe.isEmpty()) return;
        int count = Math.min(source.stackSize(slot), input.getMaxStackSize());
        for (int i = 0; i < count && outputBuffer.isEmpty() && !isRemoved(); i++) {
            if (!processOne(server, output, recipe, sourcePos, () ->
                PylonItemRules.exactlyEqual(input, source.extractOne(slot, true)) &&
                !source.extractOne(slot, false).isEmpty())) break;
        }
    }

    private List<PylonPulverizing.Result> processingRecipe(ServerLevel server, PylonUpgrade processor,
                                                          ItemStack input, int tier) {
        if (processor == PylonUpgrade.PULVERIZE) return PylonPulverizing.find(server, input, tier);
        ItemStack result = smeltingRecipe(server, input);
        return result.isEmpty() ? List.of() : List.of(new PylonPulverizing.Result(result, 100));
    }

    private boolean processOne(ServerLevel server, BiFunction<ItemStack, Boolean, ItemStack> output,
                               List<PylonPulverizing.Result> recipe, BlockPos inputInventory, BooleanSupplier takeInput) {
        ItemStack first = recipe.get(0).stack();
        if (!output.apply(first.copyWithCount(Math.min(first.getCount(), first.getMaxStackSize())), true).isEmpty() ||
            !consumeProcessingFuel(server, inputInventory, takeInput)) return false;
        for (PylonPulverizing.Result result : recipe) {
            if (result.chance() < 100 && server.getRandom().nextInt(100) >= result.chance()) continue;
            ItemStack stack = result.stack();
            for (int count = stack.getCount(); count > 0; count -= stack.getMaxStackSize()) {
                outputBuffer.add(stack.copyWithCount(Math.min(count, stack.getMaxStackSize())));
            }
        }
        flushOutput(output);
        setChanged();
        return true;
    }

    private boolean consumeProcessingFuel(ServerLevel server, BlockPos inputInventory, BooleanSupplier takeInput) {
        PylonBlockEntityBase power = processingPower(server);
        if (power != null) {
            if (!takeInput.getAsBoolean()) return false;
            power.energy.transfer(false, SMELT_ENERGY_COST, server.getGameTime());
            power.energyChanged();
            return true;
        }
        if (smeltFuel < FURNACE_TICKS_PER_ITEM) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos source = worldPosition.relative(direction);
                if (source.equals(inputInventory) || !server.hasChunkAt(source)) continue;
                int taken = takeSmeltingFuel(server, source, direction.getOpposite());
                if (taken <= 0) continue;
                smeltFuel += taken;
                setChanged();
                if (smeltFuel >= FURNACE_TICKS_PER_ITEM) break;
            }
        }
        if (smeltFuel < FURNACE_TICKS_PER_ITEM || !takeInput.getAsBoolean()) return false;
        smeltFuel -= FURNACE_TICKS_PER_ITEM;
        return true;
    }

    private PylonBlockEntityBase processingPower(ServerLevel server) {
        if (getAvailableEnergy() >= SMELT_ENERGY_COST) return this;
        for (Direction direction : Direction.values()) {
            BlockPos source = worldPosition.relative(direction);
            if (server.hasChunkAt(source) && server.getBlockEntity(source) instanceof PylonBlockEntityBase pylon &&
                pylon.getAvailableEnergy() >= SMELT_ENERGY_COST) return pylon;
        }
        return null;
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
        if (getAvailableExperience() > 0) {
            AABB outputArea = new AABB(workTarget()).minmax(new AABB(worldPosition.relative(workDirection, 2)));
            for (ServerPlayer player : playersInArea(server, outputArea)) {
                long dispensed = PylonExperience.dispense(player, levels, getAvailableExperience());
                if (!isCreativeStorage()) experience -= dispensed;
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
            boolean advanced = false;
            for (int j = 0; j < ticks; j++) {
                BlockState plant = server.getBlockState(pos);
                if (!PylonFarming.canGrow(plant)) break;
                if (hasSuperUpgrade()) {
                    if (!PylonFarming.advanceStage(server, pos, plant)) break;
                } else {
                    plant.randomTick(server, pos, random);
                }
                advanced = true;
            }
            if (advanced) count--;
        }
    }

    private void attackMobs(ServerLevel server, int targets) {
        AABB area = new AABB(worldPosition).inflate(range());
        Vec3 center = Vec3.atCenterOf(worldPosition);
        var mobs = server.getEntitiesOfClass(Mob.class, area,
            mob -> mob.isAlive() && mob.isAttackable() && area.contains(mob.position()));
        mobs.sort(Comparator.comparingDouble(mob -> mob.distanceToSqr(center)));
        ServerPlayer automation = getAutomationPlayer(server, AUTOMATION_PROFILE);
        Vec3 previousPosition = automation.position();
        float previousYaw = automation.getYRot();
        ItemStack installed = breakTool;
        ItemStack weapon = installed.isEmpty() ? new ItemStack(Items.DIAMOND_AXE) : installed.copy();
        automation.setPos(center.x, center.y, center.z);
        try {
            for (Mob mob : mobs) {
                if (targets == 0 || isRemoved() || weapon.isEmpty()) break;
                if (!mob.isAlive() || !mayWorkAt(server, automation, mob.blockPosition())) continue;
                automation.setYRot((float) Math.toDegrees(Math.atan2(center.x - mob.getX(), mob.getZ() - center.z)));
                attackMob(automation, mob, weapon);
                targets--;
            }
            if (!isRemoved() && !installed.isEmpty() && breakTool == installed &&
                !PylonItemRules.exactlyEqual(installed, weapon)) {
                breakTool = weapon.copy();
                sync();
            }
        } finally {
            automation.setPos(previousPosition.x, previousPosition.y, previousPosition.z);
            automation.setYRot(previousYaw);
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

    private void fillPlane(ServerLevel server, int maximum) {
        if (getStoredItem().isEmpty() || !(getStoredItem().getItem() instanceof BlockItem)) return;
        int radius = range();
        BlockPos plane = worldPosition.below(3);
        for (BlockPos pos : BlockPos.betweenClosed(plane.offset(-radius, 0, -radius), plane.offset(radius, 0, radius))) {
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
        if (!outputBuffer.isEmpty()) {
            var output = workOutput(server);
            if (output != null) flushOutput(output);
            if (!outputBuffer.isEmpty()) return;
        }
        if (contents.count() >= getItemCapacity()) return;
        BlockPos target = workTarget();
        if (!server.hasChunkAt(target)) return;
        var result = breakAt(server, getAutomationPlayer(server, AUTOMATION_PROFILE), target);
        if (result != null && !isRemoved() && collectBreakDrops(result.drops(), getItemCapacity())) sync();
    }

    private PylonBreaker.Result breakAt(ServerLevel server, ServerPlayer automation, BlockPos target) {
        BlockState state = server.getBlockState(target);
        if (state.isAir() || state.getBlock() instanceof LiquidBlock || state.getDestroySpeed(server, target) < 0 ||
            !mayWorkAt(server, automation, target)) return null;
        ItemStack installed = breakTool;
        ItemStack used = installed.isEmpty() ? new ItemStack(Items.DIAMOND_PICKAXE) : installed.copy();
        var result = PylonBreaker.breakBlock(server, target, automation, used);
        if (!isRemoved() && !installed.isEmpty() && breakTool == installed &&
            !PylonItemRules.exactlyEqual(installed, result.tool())) {
            if (!result.tool().isEmpty() && !PylonItemRules.isMiningTool(result.tool())) {
                Block.popResource(server, worldPosition, result.tool());
                breakTool = ItemStack.EMPTY;
            } else {
                breakTool = result.tool().copy();
            }
            sync();
        }
        return result;
    }

    private void pumpFluids(ServerLevel server, int amount) {
        excavate(server, amount, true);
        if (getFluidAmount() == 0 || !server.hasChunkAt(workTarget())) return;
        long limit = hasBulkTransfer() ? Long.MAX_VALUE : (long) amount * 1000;
        if (server.getBlockEntity(workTarget()) instanceof PylonBlockEntityBase target) {
            moveToPylon(server, PylonWire.Type.FLUID, target, target.hasBulkTransfer() ? Long.MAX_VALUE : limit);
        } else {
            transferAdjacentExternal(server, workTarget(), PylonWire.Type.FLUID, false, limit);
        }
    }

    private void excavate(ServerLevel server, int amount, boolean fluid) {
        var output = fluid ? null : workOutput(server);
        if (!fluid) {
            if (output == null) return;
            flushOutput(output);
            if (!outputBuffer.isEmpty()) return;
        } else if (getFluidCapacity() - getFluidAmount() < 1000) {
            return;
        }

        int top = worldPosition.getY() - 3;
        int height = top - minimumY(server) + 1;
        if (height <= 0) return;
        PylonUpgrade rangeUpgrade = upgrades.get(PylonUpgrade.Slot.RANGE);
        int chunkRadius = rangeUpgrade == null ? 0 : rangeUpgrade.tier();
        int diameter = chunkRadius * 2 + 1;
        int layerSize = diameter * diameter * 256;
        int volume = layerSize * height;
        int centerChunk = chunkRadius * (diameter + 1);
        int originX = (worldPosition.getX() >> 4) - chunkRadius;
        int originZ = (worldPosition.getZ() >> 4) - chunkRadius;
        ServerPlayer automation = getAutomationPlayer(server, AUTOMATION_PROFILE);
        int cursor = excavationCursor % volume;
        int layerEnd = (cursor / layerSize + 1) * layerSize;
        int scan = Math.min(EXCAVATION_SCAN_BUDGET, layerEnd - cursor);
        for (int i = 0; i < scan && amount > 0 && !isRemoved(); i++) {
            int index = cursor++;
            int chunk = index / 256 % (diameter * diameter);
            if (chunk == 0) chunk = centerChunk;
            else if (chunk == centerChunk) chunk = 0;
            BlockPos pos = new BlockPos(((originX + chunk % diameter) << 4) + (index & 15),
                top - index / layerSize, ((originZ + chunk / diameter) << 4) + ((index >> 4) & 15));
            if (!server.hasChunkAt(pos)) continue;
            BlockState state = server.getBlockState(pos);
            if (fluid) {
                if (!state.getFluidState().isSource() || !mayWorkAt(server, automation, pos)) continue;
                if (collectFluidSource(server, automation, pos, state)) {
                    amount--;
                    if (getFluidCapacity() - getFluidAmount() < 1000) break;
                }
            } else {
                if (state.isAir() || state.is(SuperPylonsMod.SUPER_PYLON.get())) continue;
                var result = breakAt(server, automation, pos);
                if (result == null || isRemoved() || server.getBlockState(pos) == state) continue;
                amount--;
                for (ItemEntity drop : result.drops()) {
                    if (!allowsItem(drop.getItem())) continue;
                    outputBuffer.add(drop.getItem().copy());
                    drop.discard();
                }
                flushOutput(output);
                if (!outputBuffer.isEmpty()) break;
            }
        }
        if (!isRemoved()) {
            excavationCursor = cursor % volume;
            setChanged();
        }
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
            if (isCreativeStorage() && !isRemoved() && PylonItemRules.exactlyEqual(original, remaining)) remaining = ItemStack.EMPTY;
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
        return isCreativeStorage() || packedDrop || hasExperience() || energy.amount() > 0 || smeltFuel > 0 || (getStoredCount() > 0 &&
            getStoredCount() > (long) MAX_LOOSE_STACKS * getStoredItem().getMaxStackSize());
    }

    public void dropContents() {
        if (!(level instanceof ServerLevel server) || contentsDropped) return;
        contentsDropped = true;
        updateChunkLoading(server, 0);
        clearFluid();
        dropConnectedWires(server);
        clearWorkshopLinks();
        for (ItemStack output : outputBuffer) Block.popResource(level, worldPosition, output);
        outputBuffer.clear();
        packedDrop = hasPackedDrop();
        if (packedDrop) {
            Block.popResource(level, worldPosition, createPackedDrop());
            contents.restore(ItemStack.EMPTY, 0);
        } else {
            Block.popResource(level, worldPosition, breakTool);
            Block.popResource(level, worldPosition, furnaceAttachment);
            while (contents.count() > 0) {
                Block.popResource(level, worldPosition, contents.extract(Integer.MAX_VALUE));
            }
            for (PylonUpgrade upgrade : upgrades.values()) {
                Block.popResource(level, worldPosition, upgradeStack(upgrade));
            }
            for (ItemStack shelf : shelves) Block.popResource(level, worldPosition, shelf);
        }
        breakTool = ItemStack.EMPTY;
        furnaceAttachment = ItemStack.EMPTY;
        shelves.clear();
        upgrades.clear();
    }

    private void dropConnectedWires(ServerLevel server) {
        List<ItemStack> drops = new ArrayList<>();
        for (PylonWire wire : wires) {
            drops.add(wire.type().itemStack());
            if (server.hasChunkAt(wire.target()) && server.getBlockEntity(wire.target()) instanceof PylonBlockEntityBase target &&
                target.wireId.equals(wire.targetId())) {
                target.wireInputs.remove(new PylonWire(wire.type(), worldPosition, wireId));
                target.sync();
            }
        }
        wires.clear();
        for (PylonWire input : new ArrayList<>(wireInputs)) {
            server.getChunk(input.target());
            if (server.getBlockEntity(input.target()) instanceof PylonBlockEntityBase source && source.wireId.equals(input.targetId())) {
                detachWireSource(source, drops);
            }
        }
        wireInputs.clear();
        int reach = PylonWire.MAX_LENGTH;
        for (int x = (worldPosition.getX() - reach) >> 4; x <= (worldPosition.getX() + reach) >> 4; x++) {
            for (int z = (worldPosition.getZ() - reach) >> 4; z <= (worldPosition.getZ() + reach) >> 4; z++) {
                var chunk = server.getChunkSource().getChunkNow(x, z);
                if (chunk == null) continue;
                for (BlockEntity entity : new ArrayList<>(chunk.getBlockEntities().values())) {
                    if (!(entity instanceof PylonBlockEntityBase source) || source == this) continue;
                    detachWireSource(source, drops);
                }
            }
        }
        for (ItemStack wire : drops) Block.popResource(server, worldPosition, wire);
    }

    private void detachWireSource(PylonBlockEntityBase source, List<ItemStack> drops) {
        boolean detached = source.wires.removeIf(wire -> {
            if (!wire.target().equals(worldPosition) || !wire.targetId().equals(wireId)) return false;
            drops.add(wire.type().itemStack());
            return true;
        });
        if (detached) source.sync();
    }

    protected void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
}
